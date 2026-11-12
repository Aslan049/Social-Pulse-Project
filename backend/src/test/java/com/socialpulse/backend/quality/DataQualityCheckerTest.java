package com.socialpulse.backend.quality;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.IssueSeverity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.ingestion.ParsedPoint;
import com.socialpulse.backend.quality.DataQualityChecker.QualityFinding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataQualityCheckerTest {

    private final DataQualityChecker checker = new DataQualityChecker();

    private static ParsedPoint day(Metric metric, int dayOfJanuary, String value) {
        return new ParsedPoint(metric, Granularity.DAY, LocalDate.of(2026, 1, dayOfJanuary), new BigDecimal(value));
    }

    private static ParsedPoint month(Metric metric, int year, int month, String value) {
        return new ParsedPoint(metric, Granularity.MONTH, LocalDate.of(year, month, 1), new BigDecimal(value));
    }

    @Test
    void emptyInput_givesNoFindings() {
        assertTrue(checker.check(List.of()).isEmpty());
    }

    @Test
    void negativeValue_isWarnedAboutButNotRejected() {
        List<QualityFinding> findings = checker.check(List.of(
                day(Metric.ENGAGEMENT, 1, "5"), day(Metric.ENGAGEMENT, 2, "-7")));

        assertEquals(1, findings.size());
        assertEquals(IssueSeverity.WARNING, findings.get(0).severity());
        assertEquals(Metric.ENGAGEMENT, findings.get(0).metric());
        assertTrue(findings.get(0).message().startsWith("Negatif değer: 2026-01-02 = -7"));
    }

    @Test
    void negativeDay_isNotReportedAgainAsAGap() {
        List<QualityFinding> findings = checker.check(List.of(
                day(Metric.VIEWS, 1, "5"), day(Metric.VIEWS, 2, "-1"), day(Metric.VIEWS, 3, "5")));

        assertTrue(findings.stream().noneMatch(f -> f.message().contains("için veri yok")));
    }

    @Test
    void manyNegativeValues_areCappedWithSummaryLine() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 25; d++) {
            points.add(day(Metric.ENGAGEMENT, d, "-1"));
        }

        List<QualityFinding> findings = checker.check(points);

        assertEquals(21, findings.size());
        assertTrue(findings.get(20).message().contains("5 negatif değer daha"));
    }

    @Test
    void metricStartingLaterThanTheCommonRange_isAnInfoNotAMissingDataWarning() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 10; d++) {
            points.add(day(Metric.VIEWS, d, "10"));
        }
        for (int d = 6; d <= 10; d++) {
            points.add(day(Metric.NEW_FOLLOWERS, d, "1"));
        }

        List<QualityFinding> findings = checker.check(points);

        assertEquals(1, findings.size());
        assertEquals(IssueSeverity.INFO, findings.get(0).severity());
        assertEquals(Metric.NEW_FOLLOWERS, findings.get(0).metric());
        assertTrue(findings.get(0).message().startsWith(
                "Metrik 2026-01-06 tarihinde başlıyor; bu yüklemedeki ortak aralığın ilk 5 gün için veri yok"));
        assertTrue(findings.get(0).message().contains("eksik veri sayılmadı"));
    }

    @Test
    void metricEndingEarlierThanTheCommonRange_isAnInfo() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 10; d++) {
            points.add(day(Metric.VIEWS, d, "10"));
        }
        for (int d = 1; d <= 4; d++) {
            points.add(day(Metric.REACH, d, "1"));
        }

        List<QualityFinding> findings = checker.check(points);

        assertEquals(1, findings.size());
        assertEquals(IssueSeverity.INFO, findings.get(0).severity());
        assertTrue(findings.get(0).message().startsWith("Metrik 2026-01-04 tarihinde bitiyor"));
    }

    @Test
    void gapsInsideAMetricsOwnRange_areRealMissingDataWarnings() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d : new int[]{1, 2, 3, 6, 7}) {
            points.add(day(Metric.VIEWS, d, "10"));
        }

        List<QualityFinding> findings = checker.check(points);

        assertEquals(1, findings.size());
        assertEquals(IssueSeverity.WARNING, findings.get(0).severity());
        assertTrue(findings.get(0).message().startsWith(
                "2 gün için veri yok (kendi aralığı içinde: 2026-01-01 – 2026-01-07, 5/7 gün"));
    }

    @Test
    void missingMonthInsideOwnRange_isWarnedAboutForMonthlySeries() {
        List<ParsedPoint> points = List.of(
                month(Metric.VIEWS, 2025, 10, "1"), month(Metric.VIEWS, 2025, 12, "1"),
                month(Metric.LIKES, 2025, 10, "1"), month(Metric.LIKES, 2025, 11, "1"), month(Metric.LIKES, 2025, 12, "1"));

        List<QualityFinding> findings = checker.check(points);

        assertEquals(1, findings.size());
        assertEquals(Metric.VIEWS, findings.get(0).metric());
        assertEquals(IssueSeverity.WARNING, findings.get(0).severity());
        assertTrue(findings.get(0).message().startsWith("1 ay için veri yok"));
    }

    @Test
    void valueFarAboveTheMedian_isWarnedAbout() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 20; d++) {
            points.add(day(Metric.REACH, d, "100"));
        }
        points.add(day(Metric.REACH, 21, "50000"));

        List<QualityFinding> findings = checker.check(points);

        assertEquals(1, findings.size());
        assertTrue(findings.get(0).message().startsWith("Aşırı uç değer: 2026-01-21 = 50000 (medyanın yaklaşık 500 katı)"));
    }

    @Test
    void extremeValueCheck_needsEnoughPoints() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 5; d++) {
            points.add(day(Metric.REACH, d, d == 5 ? "100000" : "10"));
        }

        assertTrue(checker.check(points).isEmpty());
    }

    @Test
    void completeCleanSeries_givesNoFindings() {
        List<ParsedPoint> points = new ArrayList<>();
        for (int d = 1; d <= 15; d++) {
            points.add(day(Metric.VIEWS, d, String.valueOf(100 + d)));
            points.add(day(Metric.ENGAGEMENT, d, String.valueOf(10 + d)));
        }

        assertTrue(checker.check(points).isEmpty());
    }
}
