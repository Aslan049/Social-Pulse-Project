package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.exception.InvalidUploadException;

import java.util.List;

/**
 * Platform adaptörlerinin ortak gövdesi. Platforma özgü olan yalnızca beklenen zaman çözünürlüğü ve
 * adıdır; tablo düzeni (uzun veya geniş) dosyadan tanınır, böylece XLSX ve CSV aynı yoldan geçer.
 */
abstract class AbstractTableAdapter implements PlatformAdapter {

    protected abstract Granularity expectedGranularity();

    protected abstract String platformLabel();

    @Override
    public ParseResult parse(List<RawTable> tables) {
        ParseAccumulator acc = new ParseAccumulator();
        boolean anyTable = false;

        for (RawTable table : tables) {
            if (TableLayouts.isDerivedSheet(table.name())) {
                acc.ignoreSheet(table.name());
                continue;
            }
            TableLayouts.Layout layout = TableLayouts.detect(table);
            if (layout == null) {
                acc.ignoreSheet(table.name());
                if (!table.rows().isEmpty() && MetricAliases.find(table.name()).isPresent()) {
                    acc.issue(table.name(), 1, "Başlık satırı tanınmadı (Tarih ve Değer sütunları bulunamadı)");
                }
                continue;
            }

            Granularity actual = TableLayouts.infer(table, layout, expectedGranularity());
            if (actual != expectedGranularity()) {
                acc.ignoreSheet(table.name());
                acc.issue(table.name(), 1, actual == Granularity.DAY
                        ? "Günlük veri gibi görünüyor; " + platformLabel() + " için aylık veri bekleniyor. Sayfa okunmadı"
                        : "Aylık veri gibi görünüyor; " + platformLabel() + " için günlük veri bekleniyor. Sayfa okunmadı");
                continue;
            }

            Metric longMetric = null;
            if (layout.kind() == TableLayouts.Kind.LONG) {
                longMetric = MetricAliases.find(table.name()).orElse(null);
                if (longMetric == null) {
                    acc.ignoreSheet(table.name());
                    continue;
                }
            }
            anyTable = true;
            MetricTableParser.parse(table, layout, actual, longMetric, acc);
        }

        if (!anyTable) {
            throw new InvalidUploadException(platformLabel() + " dosyasında okunabilir bir metrik tablosu bulunamadı. "
                    + "Bulunan sayfalar: " + String.join(", ", tables.stream().map(RawTable::name).limit(10).toList())
                    + ". Tablo, bir tarih sütunu ile metrik adı taşıyan sütun(lar) içermeli; iki sütunlu (Tarih, Değer) "
                    + "tabloların metriği sayfa veya dosya adından (örn. ziyaretler) tanınır.");
        }
        return acc.toResult();
    }
}
