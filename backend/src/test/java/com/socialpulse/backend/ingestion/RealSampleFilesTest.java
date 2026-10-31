package com.socialpulse.backend.ingestion;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gerçek biçimli iki örnek dosya (X aylık özet, Instagram günlük seri) üzerinde okuyucu testi.
 * Bu testler, adaptörlerin (Gün 6-7) dayanacağı ham tablo yapısını sabitler.
 */
class RealSampleFilesTest {

    private final XlsxTabularReader reader = new XlsxTabularReader();

    private List<RawTable> load(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/" + name)) {
            assertNotNull(in, name + " test kaynaklarında bulunamadı");
            return reader.read(in);
        }
    }

    private RawTable sheet(List<RawTable> tables, String name) {
        return tables.stream().filter(t -> t.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Sayfa yok: " + name));
    }

    private double number(Object cell) {
        return ((Number) cell).doubleValue();
    }

    @Test
    void xFile_isMonthlySummary() throws Exception {
        List<RawTable> tables = load("x_aylik_ozet.xlsx");

        assertEquals(1, tables.size());
        RawTable table = tables.get(0);
        assertEquals("Aylık Özet", table.name());
        assertEquals(12, table.rowCount(), "1 başlık + 11 ay");
        assertEquals("Ay", table.headerPreview().get(0));
        assertEquals("Görüntülenmeler", table.headerPreview().get(1));
        assertEquals(LocalDate.of(2025, 10, 1).atStartOfDay(), table.rows().get(1).get(0));
        assertEquals(3255.0, number(table.rows().get(1).get(1)));
    }

    @Test
    void instagramFile_hasOneSheetPerMetric() throws Exception {
        List<RawTable> tables = load("instagram_gunluk.xlsx");

        assertEquals(
                List.of("İstatistikler", "Ziyaretler", "Etkileşim", "Erişim", "Takipler",
                        "Görüntülemeler", "Bağlantı Tıklamaları", "Tüm Veriler"),
                tables.stream().map(RawTable::name).toList());

        RawTable visits = sheet(tables, "Ziyaretler");
        assertEquals(366, visits.rowCount(), "1 başlık + 365 gün");
        assertEquals(List.of("Tarih", "Değer"), visits.headerPreview());
        assertEquals(LocalDate.of(2025, 9, 1).atStartOfDay(), visits.rows().get(1).get(0));
        assertEquals(1883.0, number(visits.rows().get(1).get(1)));
    }

    @Test
    void followsSheet_hasMissingDaysAsMissingRows() throws Exception {
        List<RawTable> tables = load("instagram_gunluk.xlsx");

        RawTable follows = sheet(tables, "Takipler");
        assertEquals(322, follows.rowCount(), "321 gün: 15 Ekim 2025'ten önceki günler satır olarak yok");
        assertEquals(LocalDate.of(2025, 10, 15).atStartOfDay(), follows.rows().get(1).get(0));
    }

    @Test
    void combinedSheet_fillsMissingDaysWithZero_whichWeMustNotTrust() throws Exception {
        List<RawTable> tables = load("instagram_gunluk.xlsx");

        RawTable all = sheet(tables, "Tüm Veriler");
        assertEquals(366, all.rowCount());
        long zeroFollowDays = all.rows().stream().skip(1)
                .filter(row -> number(row.get(4)) == 0.0)
                .count();
        assertEquals(44, zeroFollowDays, "Birleşik sayfada eksik 44 gün 0 olarak doldurulmuş");
    }

    @Test
    void engagementSheet_containsNegativeValue() throws Exception {
        List<RawTable> tables = load("instagram_gunluk.xlsx");

        RawTable engagement = sheet(tables, "Etkileşim");
        boolean hasNegative = engagement.rows().stream().skip(1).anyMatch(row -> number(row.get(1)) < 0);
        assertTrue(hasNegative, "Doğrulama katmanı bu negatif değeri yakalayabilmeli");
    }
}
