package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.exception.InvalidUploadException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * X aylık özet dosyası: tek geniş tablo (satır = ay, sütun = metrik).
 * Ay sütunu başlıktan ("Ay", "Tarih", "Month"...), metrik sütunları başlık adlarından tanınır;
 * tanınmayan sütunlar yok sayılır ve bildirilir. Boş hücre "veri yok" demektir (0 yazılmaz, hata sayılmaz).
 */
@Component
public class XAdapter implements PlatformAdapter {

    private static final Set<String> MONTH_HEADERS = Set.of("ay", "month", "tarih", "date", "donem", "period");

    @Override
    public Platform platform() {
        return Platform.X;
    }

    @Override
    public ParseResult parse(List<RawTable> tables) {
        List<ParsedPoint> points = new ArrayList<>();
        List<ParseIssue> issues = new ArrayList<>();
        List<String> ignoredSheets = new ArrayList<>();
        List<String> ignoredColumns = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int dataRows = 0;
        boolean anyTable = false;

        for (RawTable table : tables) {
            Layout layout = locate(table);
            if (layout == null) {
                ignoredSheets.add(table.name());
                continue;
            }
            if (looksDaily(table, layout)) {
                // Günlük bir seriyi aya çökertmek sessizce yanlış değer üretirdi; bu tabloyu reddet
                ignoredSheets.add(table.name());
                issues.add(new ParseIssue(table.name(), 1,
                        "Günlük veri gibi görünüyor; X için aylık özet bekleniyor. Sayfa okunmadı"));
                continue;
            }
            anyTable = true;
            layout.unknownHeaders().forEach(h -> ignoredColumns.add(table.name() + ": " + h));

            for (int i = 1; i < table.rows().size(); i++) {
                List<Object> row = table.rows().get(i);
                int rowNumber = i + 1;

                LocalDate month;
                try {
                    month = CellParser.toMonth(cell(row, layout.monthColumn()));
                } catch (IllegalArgumentException e) {
                    issues.add(new ParseIssue(table.name(), rowNumber, e.getMessage()));
                    continue;
                }

                int examined = 0;
                for (Map.Entry<Integer, Metric> column : layout.metricColumns().entrySet()) {
                    String header = String.valueOf(table.rows().get(0).get(column.getKey()));

                    BigDecimal value;
                    try {
                        value = CellParser.toNumber(cell(row, column.getKey()));
                    } catch (IllegalArgumentException e) {
                        examined++;
                        dataRows++;
                        issues.add(new ParseIssue(table.name(), rowNumber, header + ": " + e.getMessage()));
                        continue;
                    }
                    if (value == null) {
                        continue; // veri yok: nokta üretilmez, hata da sayılmaz
                    }
                    examined++;
                    dataRows++;

                    if (!seen.add(column.getValue() + "|" + month)) {
                        issues.add(new ParseIssue(table.name(), rowNumber, header + ": Tekrar eden ay: "
                                + month.getYear() + "-" + String.format("%02d", month.getMonthValue())));
                        continue;
                    }
                    points.add(new ParsedPoint(column.getValue(), Granularity.MONTH, month, value));
                }
                if (examined == 0) {
                    issues.add(new ParseIssue(table.name(), rowNumber, "Satırda ölçüm değeri yok"));
                }
            }
        }

        if (!anyTable) {
            throw new InvalidUploadException(
                    "X dosyasında ay sütunu ve tanınan metrik sütunları olan bir tablo bulunamadı. Bulunan sayfalar: "
                            + String.join(", ", tables.stream().map(RawTable::name).limit(10).toList()));
        }
        return new ParseResult(points, issues, ignoredSheets, ignoredColumns, dataRows);
    }

    /** Aynı ayda birden fazla farklı gün varsa tablo günlüktür (aylık özet değildir). */
    private static boolean looksDaily(RawTable table, Layout layout) {
        Map<LocalDate, Set<LocalDate>> daysPerMonth = new HashMap<>();
        for (int i = 1; i < table.rows().size(); i++) {
            LocalDate day = fullDateOrNull(cell(table.rows().get(i), layout.monthColumn()));
            if (day == null) {
                continue;
            }
            Set<LocalDate> days = daysPerMonth.computeIfAbsent(day.withDayOfMonth(1), k -> new HashSet<>());
            days.add(day);
            if (days.size() > 1) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate fullDateOrNull(Object cell) {
        if (cell instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (cell instanceof LocalDate date) {
            return date;
        }
        if (cell instanceof String) {
            try {
                return CellParser.toDate(cell);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /** Başlık satırından ay sütununu ve metrik sütunlarını bulur; uygun değilse null. */
    private static Layout locate(RawTable table) {
        if (table.rows().isEmpty()) {
            return null;
        }
        List<Object> header = table.rows().get(0);
        int monthColumn = -1;
        Map<Integer, Metric> metricColumns = new LinkedHashMap<>();
        Set<Metric> mapped = new HashSet<>();
        List<String> unknown = new ArrayList<>();

        for (int i = 0; i < header.size(); i++) {
            Object cell = header.get(i);
            if (cell == null) {
                continue;
            }
            String text = String.valueOf(cell);
            if (monthColumn < 0 && MONTH_HEADERS.contains(MetricAliases.normalize(text))) {
                monthColumn = i;
                continue;
            }
            Metric metric = MetricAliases.find(text).orElse(null);
            if (metric != null && mapped.add(metric)) {
                metricColumns.put(i, metric);
            } else {
                unknown.add(text);
            }
        }
        if (monthColumn < 0 || metricColumns.isEmpty()) {
            return null;
        }
        return new Layout(monthColumn, metricColumns, unknown);
    }

    private static Object cell(List<Object> row, int index) {
        return index < row.size() ? row.get(index) : null;
    }

    private record Layout(int monthColumn, Map<Integer, Metric> metricColumns, List<String> unknownHeaders) {}
}
