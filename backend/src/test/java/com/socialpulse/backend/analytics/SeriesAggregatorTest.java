package com.socialpulse.backend.analytics;

import com.socialpulse.backend.analytics.SeriesAggregator.MonthValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeriesAggregatorTest {

    private static List<StoredPoint> sampleDays() {
        List<StoredPoint> days = new ArrayList<>();
        for (int d = 1; d <= 31; d++) {
            if (d != 10) {
                days.add(new StoredPoint(LocalDate.of(2026, 1, d), new BigDecimal("1")));
            }
        }
        for (int d = 1; d <= 15; d++) {
            days.add(new StoredPoint(LocalDate.of(2026, 2, d), new BigDecimal("2")));
        }
        return days;
    }

    @Test
    void daysAreSummedPerMonthWithCoverage() {
        List<MonthValue> months = SeriesAggregator.toMonths(sampleDays(), null, null);

        assertEquals(2, months.size());
        assertEquals(LocalDate.of(2026, 1, 1), months.get(0).month());
        assertEquals(0, new BigDecimal("30").compareTo(months.get(0).total()));
        assertEquals(30, months.get(0).observedDays());
        assertEquals(31, months.get(0).expectedDays());
        assertEquals(30.0 / 31, months.get(0).coverage(), 1e-9);
    }

    @Test
    void partialMonthWithoutRange_isExpectedToBeFull() {
        MonthValue february = SeriesAggregator.toMonths(sampleDays(), null, null).get(1);

        assertEquals(0, new BigDecimal("30").compareTo(february.total()));
        assertEquals(28, february.expectedDays());
        assertEquals(15.0 / 28, february.coverage(), 1e-9);
    }

    @Test
    void rangeLimitsTheExpectedDays() {
        List<MonthValue> months = SeriesAggregator.toMonths(
                sampleDays(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 15));

        assertEquals(15, months.get(1).expectedDays());
        assertEquals(1.0, months.get(1).coverage());
    }

    @Test
    void rangeStartInsideMonth_limitsExpectedDays() {
        List<StoredPoint> fromTwentieth = sampleDays().stream()
                .filter(p -> !p.periodStart().isBefore(LocalDate.of(2026, 1, 20)))
                .toList();

        List<MonthValue> months = SeriesAggregator.toMonths(fromTwentieth, LocalDate.of(2026, 1, 20), null);

        assertEquals(12, months.get(0).expectedDays());
    }

    @Test
    void emptyInput_givesEmptyResult() {
        assertTrue(SeriesAggregator.toMonths(List.of(), null, null).isEmpty());
    }
}
