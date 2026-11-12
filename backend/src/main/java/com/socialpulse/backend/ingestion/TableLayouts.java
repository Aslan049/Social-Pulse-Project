package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;

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
 * Bir ham tablonun düzenini tanır.
 * <ul>
 *   <li>UZUN (LONG): iki sütun (tarih, değer); metrik sayfa veya dosya adından gelir.</li>
 *   <li>GENİŞ (WIDE): tarih sütunu + metrik adı taşıyan bir veya daha fazla sütun.</li>
 * </ul>
 */
final class TableLayouts {

    enum Kind { LONG, WIDE }

    record Layout(Kind kind, int dateColumn, int valueColumn,
                  Map<Integer, Metric> metricColumns, List<String> unknownHeaders) {}

    private static final Set<String> DATE_HEADERS =
            Set.of("tarih", "date", "gun", "day", "ay", "month", "donem", "period");
    private static final Set<String> VALUE_HEADERS = Set.of("deger", "value", "sayi", "adet");

    /**
     * Başka sayfalardan türetilmiş sayfalar. Örneğin "Tüm Veriler" sayfasında eksik günler 0 olarak
     * doldurulduğundan güvenilmezdir. "Aylık Özet" gibi gerçek veri sayfaları bu listede değildir.
     */
    private static final Set<String> DERIVED_SHEETS =
            Set.of("tumveriler", "alldata", "istatistikler", "statistics", "summary", "ozet");

    private TableLayouts() {
    }

    static boolean isDerivedSheet(String name) {
        return DERIVED_SHEETS.contains(MetricAliases.normalize(name));
    }

    /** Başlık satırından düzeni bulur; tanınmazsa null. */
    static Layout detect(RawTable table) {
        if (table.rows().isEmpty()) {
            return null;
        }
        List<Object> header = table.rows().get(0);
        int dateColumn = -1;
        int valueColumn = -1;
        String valueHeader = null;
        Map<Integer, Metric> metricColumns = new LinkedHashMap<>();
        Set<Metric> mapped = new HashSet<>();
        List<String> unknown = new ArrayList<>();

        for (int i = 0; i < header.size(); i++) {
            Object cell = header.get(i);
            if (cell == null) {
                continue;
            }
            String text = String.valueOf(cell);
            String key = MetricAliases.normalize(text);
            if (dateColumn < 0 && DATE_HEADERS.contains(key)) {
                dateColumn = i;
            } else if (valueColumn < 0 && VALUE_HEADERS.contains(key)) {
                valueColumn = i;
                valueHeader = text;
            } else {
                Metric metric = MetricAliases.find(text).orElse(null);
                if (metric != null && mapped.add(metric)) {
                    metricColumns.put(i, metric);
                } else {
                    unknown.add(text);
                }
            }
        }
        if (dateColumn < 0) {
            return null;
        }
        if (!metricColumns.isEmpty()) {
            if (valueHeader != null) {
                unknown.add(valueHeader);
            }
            return new Layout(Kind.WIDE, dateColumn, -1, metricColumns, unknown);
        }
        if (valueColumn >= 0) {
            return new Layout(Kind.LONG, dateColumn, valueColumn, Map.of(), unknown);
        }
        return null;
    }

    /**
     * Tablonun zaman çözünürlüğünü tarih sütunundan çıkarır. Aynı ayda birden fazla farklı gün varsa
     * günlük; yalnızca ay bilgisi (veya her biri ayın 1'i olan en az 3 tarih) varsa aylık; aksi halde
     * belirsizdir ve fallback döner.
     */
    static Granularity infer(RawTable table, Layout layout, Granularity fallback) {
        Map<LocalDate, Set<LocalDate>> daysPerMonth = new HashMap<>();
        int fullDates = 0;
        int monthOnly = 0;
        boolean allFirstOfMonth = true;

        for (int i = 1; i < table.rows().size(); i++) {
            Object cell = cell(table.rows().get(i), layout.dateColumn());
            LocalDate full = fullDateOrNull(cell);
            if (full != null) {
                fullDates++;
                if (full.getDayOfMonth() != 1) {
                    allFirstOfMonth = false;
                }
                Set<LocalDate> days = daysPerMonth.computeIfAbsent(full.withDayOfMonth(1), k -> new HashSet<>());
                days.add(full);
                if (days.size() > 1) {
                    return Granularity.DAY;
                }
            } else if (cell instanceof String && isMonthOnly(cell)) {
                monthOnly++;
            }
        }
        if (fullDates == 0 && monthOnly > 0) {
            return Granularity.MONTH;
        }
        if (fullDates >= 3 && monthOnly == 0 && allFirstOfMonth) {
            return Granularity.MONTH;
        }
        return fallback;
    }

    static Object cell(List<Object> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index) : null;
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

    private static boolean isMonthOnly(Object cell) {
        try {
            CellParser.toMonth(cell);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
