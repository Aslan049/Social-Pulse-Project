package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.exception.InvalidUploadException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Instagram günlük seri dosyası: her metrik için ayrı bir sayfa (Tarih, Değer).
 * Sayfa adı metriği belirler. Özet ("İstatistikler") ve birleşik ("Tüm Veriler") sayfalar
 * metrik adı olmadığı için okunmaz; birleşik sayfada eksik günler 0 yazıldığından zaten güvenilmezdir.
 * Eksik gün için satır üretilmez.
 */
@Component
public class InstagramAdapter implements PlatformAdapter {

    private static final Set<String> DATE_HEADERS = Set.of("tarih", "date", "gun", "day");
    private static final Set<String> VALUE_HEADERS = Set.of("deger", "value", "sayi", "adet");

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public ParseResult parse(List<RawTable> tables) {
        List<ParsedPoint> points = new ArrayList<>();
        List<ParseIssue> issues = new ArrayList<>();
        List<String> ignoredSheets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int dataRows = 0;
        boolean anyMetricSheet = false;

        for (RawTable table : tables) {
            Optional<Metric> metric = MetricAliases.find(table.name());
            if (metric.isEmpty()) {
                ignoredSheets.add(table.name());
                continue;
            }
            int[] columns = locateColumns(table);
            if (columns == null) {
                ignoredSheets.add(table.name());
                issues.add(new ParseIssue(table.name(), 1,
                        "Başlık satırı tanınmadı (Tarih ve Değer sütunları bulunamadı)"));
                continue;
            }
            anyMetricSheet = true;

            for (int i = 1; i < table.rows().size(); i++) {
                dataRows++;
                List<Object> row = table.rows().get(i);
                try {
                    LocalDate date = CellParser.toDate(cell(row, columns[0]));
                    BigDecimal value = CellParser.toNumber(cell(row, columns[1]));
                    if (value == null) {
                        throw new IllegalArgumentException("Değer boş, satır atlandı");
                    }
                    if (!seen.add(metric.get() + "|" + date)) {
                        throw new IllegalArgumentException("Tekrar eden tarih: " + date);
                    }
                    points.add(new ParsedPoint(metric.get(), Granularity.DAY, date, value));
                } catch (IllegalArgumentException e) {
                    issues.add(new ParseIssue(table.name(), i + 1, e.getMessage()));
                }
            }
        }

        if (!anyMetricSheet) {
            throw new InvalidUploadException(
                    "Instagram dosyasında tanınan bir metrik sayfası bulunamadı. Bulunan sayfalar: "
                            + String.join(", ", tables.stream().map(RawTable::name).limit(10).toList()));
        }
        return new ParseResult(points, issues, ignoredSheets, dataRows);
    }

    /** İlk satırdan tarih ve değer sütunlarının yerini bulur; bulunamazsa null. */
    private static int[] locateColumns(RawTable table) {
        if (table.rows().isEmpty()) {
            return null;
        }
        List<Object> header = table.rows().get(0);
        int dateColumn = -1;
        int valueColumn = -1;
        for (int i = 0; i < header.size(); i++) {
            Object cell = header.get(i);
            if (cell == null) {
                continue;
            }
            String key = MetricAliases.normalize(String.valueOf(cell));
            if (dateColumn < 0 && DATE_HEADERS.contains(key)) {
                dateColumn = i;
            } else if (valueColumn < 0 && VALUE_HEADERS.contains(key)) {
                valueColumn = i;
            }
        }
        return dateColumn >= 0 && valueColumn >= 0 ? new int[]{dateColumn, valueColumn} : null;
    }

    private static Object cell(List<Object> row, int index) {
        return index < row.size() ? row.get(index) : null;
    }
}
