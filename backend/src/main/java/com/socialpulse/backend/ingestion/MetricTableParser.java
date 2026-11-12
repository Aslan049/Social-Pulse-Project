package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Düzeni tanınmış bir tablonun satırlarını ölçümlere çevirir. */
final class MetricTableParser {

    private MetricTableParser() {
    }

    static void parse(RawTable table, TableLayouts.Layout layout, Granularity granularity,
                      Metric longMetric, ParseAccumulator acc) {
        layout.unknownHeaders().forEach(h -> acc.ignoredColumns.add(table.name() + ": " + h));
        if (layout.kind() == TableLayouts.Kind.LONG) {
            parseLong(table, layout, granularity, longMetric, acc);
        } else {
            parseWide(table, layout, granularity, acc);
        }
    }

    /** Uzun düzen: her satır tek bir ölçüm; boş değer atlanır ve raporlanır. */
    private static void parseLong(RawTable table, TableLayouts.Layout layout, Granularity granularity,
                                  Metric metric, ParseAccumulator acc) {
        for (int i = 1; i < table.rows().size(); i++) {
            acc.dataRows++;
            List<Object> row = table.rows().get(i);
            try {
                LocalDate date = parseDate(TableLayouts.cell(row, layout.dateColumn()), granularity);
                BigDecimal value = CellParser.toNumber(TableLayouts.cell(row, layout.valueColumn()));
                if (value == null) {
                    throw new IllegalArgumentException("Değer boş, satır atlandı");
                }
                if (!acc.seen.add(metric + "|" + date)) {
                    throw new IllegalArgumentException(duplicateMessage(date, granularity));
                }
                acc.points.add(new ParsedPoint(metric, granularity, date, value));
            } catch (IllegalArgumentException e) {
                acc.issue(table.name(), i + 1, e.getMessage());
            }
        }
    }

    /**
     * Geniş düzen: satır = dönem, sütun = metrik. Boş hücre "veri yok" demektir (nokta üretilmez,
     * hata sayılmaz). Bozuk tarih tüm satırı, bozuk sayı yalnızca o hücreyi reddeder.
     */
    private static void parseWide(RawTable table, TableLayouts.Layout layout, Granularity granularity,
                                  ParseAccumulator acc) {
        List<Object> header = table.rows().get(0);
        for (int i = 1; i < table.rows().size(); i++) {
            List<Object> row = table.rows().get(i);
            int rowNumber = i + 1;

            LocalDate period;
            try {
                period = parseDate(TableLayouts.cell(row, layout.dateColumn()), granularity);
            } catch (IllegalArgumentException e) {
                acc.issue(table.name(), rowNumber, e.getMessage());
                continue;
            }

            int examined = 0;
            for (Map.Entry<Integer, Metric> column : layout.metricColumns().entrySet()) {
                String columnName = String.valueOf(header.get(column.getKey()));
                BigDecimal value;
                try {
                    value = CellParser.toNumber(TableLayouts.cell(row, column.getKey()));
                } catch (IllegalArgumentException e) {
                    examined++;
                    acc.dataRows++;
                    acc.issue(table.name(), rowNumber, columnName + ": " + e.getMessage());
                    continue;
                }
                if (value == null) {
                    continue;
                }
                examined++;
                acc.dataRows++;
                if (!acc.seen.add(column.getValue() + "|" + period)) {
                    acc.issue(table.name(), rowNumber, columnName + ": " + duplicateMessage(period, granularity));
                    continue;
                }
                acc.points.add(new ParsedPoint(column.getValue(), granularity, period, value));
            }
            if (examined == 0) {
                acc.issue(table.name(), rowNumber, "Satırda ölçüm değeri yok");
            }
        }
    }

    private static LocalDate parseDate(Object cell, Granularity granularity) {
        return granularity == Granularity.DAY ? CellParser.toDate(cell) : CellParser.toMonth(cell);
    }

    private static String duplicateMessage(LocalDate date, Granularity granularity) {
        return granularity == Granularity.DAY
                ? "Tekrar eden tarih: " + date
                : "Tekrar eden ay: " + date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }
}
