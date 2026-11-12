package com.socialpulse.backend.service;

import com.socialpulse.backend.analytics.StoredPoint;
import com.socialpulse.backend.dto.CellStatus;
import com.socialpulse.backend.dto.MetricSummary;
import com.socialpulse.backend.dto.MonthlyChangeResponse;
import com.socialpulse.backend.dto.MonthlyChangeRow;
import com.socialpulse.backend.dto.PlatformComparison;
import com.socialpulse.backend.dto.PlatformSeries;
import com.socialpulse.backend.dto.SeriesResponse;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.exception.InvalidRequestException;
import com.socialpulse.backend.repository.MetricPointReader;
import com.socialpulse.backend.repository.MetricPointReader.SummaryRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock MetricPointReader reader;
    @InjectMocks AnalyticsService service;

    private static StoredPoint point(LocalDate date, String value) {
        return new StoredPoint(date, new BigDecimal(value));
    }

    private static SummaryRow row(Platform platform, Metric metric, Granularity granularity, long periods,
                                  String total, LocalDate first, LocalDate last) {
        return new SummaryRow(platform, metric, granularity, periods, 0, new BigDecimal(total),
                new BigDecimal("1"), new BigDecimal("9"), first, last);
    }

    private static List<StoredPoint> days(LocalDate from, LocalDate to, String value) {
        List<StoredPoint> points = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            points.add(point(d, value));
        }
        return points;
    }

    // ------------------------------------------------------------------ series

    @Test
    void dailySeries_isReturnedAsStoredWithoutCoverage() {
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, null, null))
                .thenReturn(List.of(point(LocalDate.of(2026, 1, 1), "10"), point(LocalDate.of(2026, 1, 2), "20")));

        SeriesResponse response = service.series(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, null, null);

        assertEquals(2, response.points().size());
        assertNull(response.points().get(0).coverage());
    }

    @Test
    void monthlySeries_usesStoredMonthlyValuesWhenAvailable() {
        when(reader.findPoints(1L, Platform.X, Metric.VIEWS, Granularity.MONTH, null, null))
                .thenReturn(List.of(point(LocalDate.of(2025, 10, 1), "3255")));

        SeriesResponse response = service.series(1L, Platform.X, Metric.VIEWS, Granularity.MONTH, null, null);

        assertEquals(1, response.points().size());
        assertNull(response.points().get(0).coverage());
        verify(reader, never()).findPoints(1L, Platform.X, Metric.VIEWS, Granularity.DAY, null, null);
    }

    @Test
    void monthlySeries_aggregatesDailyValuesAndReportsCoverage() {
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.MONTH, null, null)).thenReturn(List.of());
        List<StoredPoint> januaryWithoutTenth = new ArrayList<>(days(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "1"));
        januaryWithoutTenth.removeIf(p -> p.periodStart().getDayOfMonth() == 10);
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, null, null)).thenReturn(januaryWithoutTenth);

        SeriesResponse response = service.series(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.MONTH, null, null);

        assertEquals(1, response.points().size());
        assertEquals(0, new BigDecimal("30").compareTo(response.points().get(0).value()));
        assertEquals(0.9677, response.points().get(0).coverage(), 1e-9);
    }

    @Test
    void invalidRange_isRejectedEverywhere() {
        LocalDate later = LocalDate.of(2026, 2, 1);
        LocalDate earlier = LocalDate.of(2026, 1, 1);

        assertThrows(InvalidRequestException.class,
                () -> service.series(1L, Platform.X, Metric.VIEWS, Granularity.DAY, later, earlier));
        assertThrows(InvalidRequestException.class, () -> service.summary(1L, null, later, earlier));
        assertThrows(InvalidRequestException.class, () -> service.platformComparison(1L, later, earlier));
        assertThrows(InvalidRequestException.class, () -> service.monthlyChange(1L, Platform.X, Metric.VIEWS, later, earlier));
    }

    // ------------------------------------------------------------------ summary

    @Test
    void summary_separatesLateStartFromRealGapsUsingTheCommonWindow() {
        when(reader.summarize(1L, null, null, null)).thenReturn(List.of(
                row(Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, 365, "1000", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31)),
                row(Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, Granularity.DAY, 321, "900", LocalDate.of(2025, 10, 15), LocalDate.of(2026, 8, 31)),
                row(Platform.X, Metric.VIEWS, Granularity.MONTH, 11, "5000", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 8, 1))));

        List<MetricSummary> summary = service.summary(1L, null, null, null);

        MetricSummary followers = summary.get(1);
        assertEquals(1.0, followers.coverage(), "Kendi aralığında boşluk yok");
        assertEquals(0, followers.internalMissing());
        assertEquals(44, followers.leadingMissing());
        assertEquals(0.8795, followers.windowCoverage(), 1e-9);
        assertEquals(LocalDate.of(2025, 9, 1), followers.windowStart());
        assertEquals(LocalDate.of(2026, 8, 31), followers.windowEnd());

        MetricSummary xViews = summary.get(2);
        assertEquals(1.0, xViews.coverage());
        assertEquals(1, xViews.leadingMissing(), "X'te pencerenin ilk ayı (Eylül) yok");
        assertEquals(0.9167, xViews.windowCoverage(), 1e-9);
    }

    @Test
    void summary_computesAverageAndReportsNegativeCount() {
        when(reader.summarize(1L, Platform.INSTAGRAM, null, null)).thenReturn(List.of(
                new SummaryRow(Platform.INSTAGRAM, Metric.ENGAGEMENT, Granularity.DAY, 4, 1,
                        new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("40"),
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5))));

        MetricSummary summary = service.summary(1L, Platform.INSTAGRAM, null, null).get(0);

        assertEquals(0, new BigDecimal("25.00").compareTo(summary.average()));
        assertEquals(1, summary.negativeCount());
        assertEquals(1, summary.internalMissing(), "Negatif gün eksik dönem gibi davranır");
        assertEquals(0.8, summary.coverage(), 1e-9);
    }

    @Test
    void summary_withoutData_isEmpty() {
        when(reader.summarize(1L, null, null, null)).thenReturn(List.of());

        assertTrue(service.summary(1L, null, null, null).isEmpty());
    }

    // ------------------------------------------------------------------ platform comparison

    @Test
    void platformComparison_showsTheMonthThatOnePlatformLacks() {
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        LocalDate feb28 = LocalDate.of(2026, 2, 28);
        when(reader.summarize(1L, null, null, null)).thenReturn(List.of(
                row(Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, 59, "59", jan1, feb28),
                row(Platform.INSTAGRAM, Metric.REACH, Granularity.DAY, 59, "59", jan1, feb28),
                row(Platform.X, Metric.VIEWS, Granularity.MONTH, 1, "100", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1))));
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, null, null))
                .thenReturn(days(jan1, feb28, "1"));
        when(reader.findPoints(1L, Platform.X, Metric.VIEWS, Granularity.MONTH, null, null))
                .thenReturn(List.of(point(LocalDate.of(2026, 2, 1), "100")));

        PlatformComparison comparison = service.platformComparison(1L, null, null);

        assertEquals(jan1, comparison.windowStart());
        assertEquals(feb28, comparison.windowEnd());
        assertEquals(List.of(jan1, LocalDate.of(2026, 2, 1)), comparison.months());
        assertEquals(1, comparison.metrics().size(), "Yalnızca iki platformda da bulunan metrik karşılaştırılır");
        assertEquals(Metric.VIEWS, comparison.metrics().get(0).metric());

        PlatformSeries instagram = comparison.metrics().get(0).platforms().get(0);
        PlatformSeries x = comparison.metrics().get(0).platforms().get(1);
        assertEquals(Platform.INSTAGRAM, instagram.platform());
        assertEquals(2, instagram.monthsWithData());
        assertTrue(instagram.missingMonths().isEmpty());
        assertEquals(Platform.X, x.platform());
        assertEquals(1, x.monthsWithData());
        assertEquals(List.of(jan1), x.missingMonths());
        assertEquals(CellStatus.MISSING, x.cells().get(0).status());
        assertNull(x.cells().get(0).value());
        assertEquals(CellStatus.OK, x.cells().get(1).status());
    }

    @Test
    void platformComparison_marksAPartiallyCoveredMonth() {
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        LocalDate jan31 = LocalDate.of(2026, 1, 31);
        when(reader.summarize(1L, null, null, null)).thenReturn(List.of(
                row(Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, Granularity.DAY, 17, "17", LocalDate.of(2026, 1, 15), jan31),
                row(Platform.X, Metric.NEW_FOLLOWERS, Granularity.MONTH, 1, "40", jan1, jan1)));
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, Granularity.DAY, null, null))
                .thenReturn(days(LocalDate.of(2026, 1, 15), jan31, "1"));
        when(reader.findPoints(1L, Platform.X, Metric.NEW_FOLLOWERS, Granularity.MONTH, null, null))
                .thenReturn(List.of(point(jan1, "40")));

        PlatformComparison comparison = service.platformComparison(1L, null, null);

        PlatformSeries instagram = comparison.metrics().get(0).platforms().get(0);
        assertEquals(List.of(jan1), instagram.partialMonths());
        assertEquals(CellStatus.PARTIAL, instagram.cells().get(0).status());
        assertEquals(0.5484, instagram.cells().get(0).coverage(), 1e-9);
    }

    @Test
    void platformComparison_withASinglePlatform_hasNothingToCompare() {
        when(reader.summarize(1L, null, null, null)).thenReturn(List.of(
                row(Platform.INSTAGRAM, Metric.VIEWS, Granularity.DAY, 10, "10", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))));

        PlatformComparison comparison = service.platformComparison(1L, null, null);

        assertTrue(comparison.metrics().isEmpty());
        assertTrue(comparison.months().isEmpty());
    }

    // ------------------------------------------------------------------ monthly change

    @Test
    void monthlyChange_computesPercentageAgainstThePreviousMonth() {
        LocalDate oct = LocalDate.of(2025, 10, 1);
        when(reader.summarize(1L, Platform.X, null, null)).thenReturn(List.of(
                row(Platform.X, Metric.VIEWS, Granularity.MONTH, 3, "450", oct, LocalDate.of(2025, 12, 1)),
                row(Platform.X, Metric.LIKES, Granularity.MONTH, 3, "30", oct, LocalDate.of(2025, 12, 1))));
        when(reader.findPoints(1L, Platform.X, Metric.VIEWS, Granularity.MONTH, null, null)).thenReturn(List.of(
                point(oct, "100"), point(LocalDate.of(2025, 11, 1), "150"), point(LocalDate.of(2025, 12, 1), "75")));

        MonthlyChangeResponse response = service.monthlyChange(1L, Platform.X, Metric.VIEWS, null, null);

        assertEquals(3, response.rows().size());
        MonthlyChangeRow first = response.rows().get(0);
        assertNull(first.changePercent());
        assertNull(first.note());
        MonthlyChangeRow november = response.rows().get(1);
        assertEquals(0, new BigDecimal("50.0").compareTo(november.changePercent()));
        assertTrue(november.comparable());
        MonthlyChangeRow december = response.rows().get(2);
        assertEquals(0, new BigDecimal("-50.0").compareTo(december.changePercent()));
    }

    @Test
    void monthlyChange_doesNotInventPercentagesForMissingOrZeroMonths() {
        LocalDate oct = LocalDate.of(2025, 10, 1);
        when(reader.summarize(1L, Platform.X, null, null)).thenReturn(List.of(
                row(Platform.X, Metric.VIEWS, Granularity.MONTH, 3, "5", oct, LocalDate.of(2026, 1, 1))));
        when(reader.findPoints(1L, Platform.X, Metric.VIEWS, Granularity.MONTH, null, null)).thenReturn(List.of(
                point(oct, "0"), point(LocalDate.of(2025, 11, 1), "5"), point(LocalDate.of(2026, 1, 1), "8")));

        MonthlyChangeResponse response = service.monthlyChange(1L, Platform.X, Metric.VIEWS, null, null);

        MonthlyChangeRow november = response.rows().get(1);
        assertNull(november.changePercent());
        assertEquals("Önceki ay 0 olduğundan yüzde hesaplanamaz", november.note());
        MonthlyChangeRow december = response.rows().get(2);
        assertEquals(CellStatus.MISSING, december.status());
        assertNull(december.changePercent());
        assertEquals("Bu ay için veri yok", december.note());
        MonthlyChangeRow january = response.rows().get(3);
        assertNull(january.changePercent());
        assertEquals("Önceki ay için veri yok", january.note());
        assertFalse(january.comparable());
    }

    @Test
    void monthlyChange_flagsPartialMonthsAsNotComparable() {
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        when(reader.summarize(1L, Platform.INSTAGRAM, null, null)).thenReturn(List.of(
                row(Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, Granularity.DAY, 48, "48", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 28))));
        when(reader.findPoints(1L, Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, Granularity.DAY, null, null))
                .thenReturn(days(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 28), "1"));

        MonthlyChangeResponse response = service.monthlyChange(1L, Platform.INSTAGRAM, Metric.NEW_FOLLOWERS, null, null);

        assertEquals(jan1, response.windowStart());
        MonthlyChangeRow february = response.rows().get(1);
        assertNotNull(february.changePercent());
        assertFalse(february.comparable());
        assertEquals("Kapsam eksik: yüzde değişim yanıltıcı olabilir", february.note());
    }

    @Test
    void monthlyChange_forAnUnknownSeries_isEmpty() {
        when(reader.summarize(1L, Platform.X, null, null)).thenReturn(List.of());

        MonthlyChangeResponse response = service.monthlyChange(1L, Platform.X, Metric.VIEWS, null, null);

        assertTrue(response.rows().isEmpty());
    }
}
