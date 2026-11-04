package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Metric;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Gerçek biçimli Instagram dosyasında okuyucu + adaptör birlikte. */
class InstagramAdapterRealFileTest {

    private ParseResult parseRealFile() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/instagram_gunluk.xlsx")) {
            assertNotNull(in, "instagram_gunluk.xlsx test kaynaklarında bulunamadı");
            List<RawTable> tables = new XlsxTabularReader().read(in);
            return new InstagramAdapter().parse(tables);
        }
    }

    @Test
    void realFile_isFullyImportedWithoutRejections() throws Exception {
        ParseResult result = parseRealFile();

        assertEquals(2146, result.points().size());
        assertEquals(2146, result.dataRows());
        assertEquals(0, result.rejectedRows());
        assertEquals(List.of("İstatistikler", "Tüm Veriler"), result.ignoredSheets());
    }

    @Test
    void realFile_countsPerMetric_missingFollowDaysAreNotZeroFilled() throws Exception {
        Map<Metric, Long> counts = parseRealFile().points().stream()
                .collect(Collectors.groupingBy(ParsedPoint::metric, Collectors.counting()));

        assertEquals(365L, counts.get(Metric.VIEWS));
        assertEquals(365L, counts.get(Metric.ENGAGEMENT));
        assertEquals(365L, counts.get(Metric.PROFILE_VISITS));
        assertEquals(365L, counts.get(Metric.REACH));
        assertEquals(365L, counts.get(Metric.LINK_CLICKS));
        assertEquals(321L, counts.get(Metric.NEW_FOLLOWERS), "Takip verisi 15 Ekim'den başlıyor, eksik günler satır olmamalı");

        LocalDate firstFollowDay = parseRealFile().points().stream()
                .filter(p -> p.metric() == Metric.NEW_FOLLOWERS)
                .map(ParsedPoint::periodStart)
                .min(LocalDate::compareTo).orElseThrow();
        assertEquals(LocalDate.of(2025, 10, 15), firstFollowDay);
    }

    @Test
    void realFile_keepsNegativeAndExtremeValuesForLaterValidation() throws Exception {
        ParseResult result = parseRealFile();

        List<ParsedPoint> negatives = result.points().stream().filter(p -> p.value().signum() < 0).toList();
        assertEquals(1, negatives.size());
        assertEquals(Metric.ENGAGEMENT, negatives.get(0).metric());

        BigDecimal maxReach = result.points().stream()
                .filter(p -> p.metric() == Metric.REACH)
                .map(ParsedPoint::value)
                .max(BigDecimal::compareTo).orElseThrow();
        assertEquals(0, new BigDecimal("1219106").compareTo(maxReach));
    }

    @Test
    void realFile_hasNoDuplicateKeys() throws Exception {
        ParseResult result = parseRealFile();

        Set<String> keys = result.points().stream()
                .map(p -> p.metric() + "|" + p.periodStart())
                .collect(Collectors.toSet());
        assertEquals(result.points().size(), keys.size());
    }
}
