package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Gerçek biçimli X aylık özet dosyasında okuyucu + adaptör birlikte. */
class XAdapterRealFileTest {

    private ParseResult parse(String resource, PlatformAdapter adapter) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/" + resource)) {
            assertNotNull(in, resource + " test kaynaklarında bulunamadı");
            return adapter.parse(new XlsxTabularReader().read(in));
        }
    }

    @Test
    void realFile_isFullyImportedAsMonthlyPoints() throws Exception {
        ParseResult result = parse("x_aylik_ozet.xlsx", new XAdapter());

        assertEquals(99, result.points().size(), "11 ay x 9 metrik");
        assertEquals(99, result.dataRows());
        assertEquals(0, result.rejectedRows());
        assertTrue(result.ignoredSheets().isEmpty());
        assertTrue(result.ignoredColumns().isEmpty());
        assertTrue(result.points().stream().allMatch(p -> p.granularity() == Granularity.MONTH));
        assertTrue(result.points().stream().allMatch(p -> p.periodStart().getDayOfMonth() == 1));
    }

    @Test
    void realFile_hasElevenMonthsForEveryMetric() throws Exception {
        ParseResult result = parse("x_aylik_ozet.xlsx", new XAdapter());

        Map<Metric, Long> counts = result.points().stream()
                .collect(Collectors.groupingBy(ParsedPoint::metric, Collectors.counting()));
        assertEquals(9, counts.size());
        assertTrue(counts.values().stream().allMatch(c -> c == 11L));

        LocalDate first = result.points().stream().map(ParsedPoint::periodStart).min(LocalDate::compareTo).orElseThrow();
        LocalDate last = result.points().stream().map(ParsedPoint::periodStart).max(LocalDate::compareTo).orElseThrow();
        assertEquals(LocalDate.of(2025, 10, 1), first);
        assertEquals(LocalDate.of(2026, 8, 1), last);
    }

    @Test
    void realFile_firstMonthValuesMatchTheWorkbook() throws Exception {
        ParseResult result = parse("x_aylik_ozet.xlsx", new XAdapter());
        LocalDate first = LocalDate.of(2025, 10, 1);

        double views = result.points().stream()
                .filter(p -> p.metric() == Metric.VIEWS && p.periodStart().equals(first))
                .findFirst().orElseThrow().value().doubleValue();
        double engagement = result.points().stream()
                .filter(p -> p.metric() == Metric.ENGAGEMENT && p.periodStart().equals(first))
                .findFirst().orElseThrow().value().doubleValue();
        assertEquals(3255.0, views);
        assertEquals(67.0, engagement);
    }

    @Test
    void instagramWorkbookUploadedAsX_isRejectedInsteadOfBeingCollapsedIntoMonths() throws Exception {
        assertThrows(com.socialpulse.backend.exception.InvalidUploadException.class,
                () -> parse("instagram_gunluk.xlsx", new XAdapter()));
    }

    @Test
    void xWorkbookUploadedAsInstagram_isRejected() throws Exception {
        assertThrows(com.socialpulse.backend.exception.InvalidUploadException.class,
                () -> parse("x_aylik_ozet.xlsx", new InstagramAdapter()));
    }

    @Test
    void bothPlatformsShareExactlyFourMetrics() throws Exception {
        List<Metric> xMetrics = parse("x_aylik_ozet.xlsx", new XAdapter()).points().stream()
                .map(ParsedPoint::metric).distinct().toList();
        List<Metric> igMetrics = parse("instagram_gunluk.xlsx", new InstagramAdapter()).points().stream()
                .map(ParsedPoint::metric).distinct().toList();

        List<Metric> common = xMetrics.stream().filter(igMetrics::contains).toList();
        assertEquals(4, common.size());
        assertTrue(common.containsAll(List.of(Metric.VIEWS, Metric.ENGAGEMENT, Metric.NEW_FOLLOWERS, Metric.PROFILE_VISITS)));
    }
}
