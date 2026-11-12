package com.socialpulse.backend.analytics;

import com.socialpulse.backend.dto.CellStatus;
import com.socialpulse.backend.dto.MonthCell;
import com.socialpulse.backend.entity.Granularity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthMatrixTest {

    private static StoredPoint point(int year, int month, int day, String value) {
        return new StoredPoint(LocalDate.of(year, month, day), new BigDecimal(value));
    }

    @Test
    void dailyData_givesOkPartialAndMissingMonths() {
        List<StoredPoint> days = new ArrayList<>();
        for (int d = 1; d <= 31; d++) {
            if (d != 10) {
                days.add(point(2026, 1, d, "1"));
            }
            days.add(point(2026, 3, d, "2"));
        }

        List<MonthCell> cells = MonthMatrix.build(days, Granularity.DAY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertEquals(3, cells.size());
        assertEquals(CellStatus.PARTIAL, cells.get(0).status());
        assertEquals(0, new BigDecimal("30").compareTo(cells.get(0).value()));
        assertEquals(0.9677, cells.get(0).coverage(), 1e-9);
        assertEquals(CellStatus.MISSING, cells.get(1).status());
        assertNull(cells.get(1).value(), "Eksik ay 0 değil, değersizdir");
        assertNull(cells.get(1).coverage());
        assertEquals(CellStatus.OK, cells.get(2).status());
        assertEquals(1.0, cells.get(2).coverage());
    }

    @Test
    void monthlyData_hasNoCoverageAndMarksAbsentMonthsMissing() {
        List<StoredPoint> months = List.of(point(2025, 10, 1, "100"), point(2025, 11, 1, "200"));

        List<MonthCell> cells = MonthMatrix.build(months, Granularity.MONTH, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31));

        assertEquals(4, cells.size());
        assertEquals(CellStatus.MISSING, cells.get(0).status());
        assertEquals(CellStatus.OK, cells.get(1).status());
        assertNull(cells.get(1).coverage());
        assertEquals(CellStatus.MISSING, cells.get(3).status());
    }

    @Test
    void windowStartingMidMonth_limitsTheExpectedDays() {
        List<StoredPoint> days = new ArrayList<>();
        for (int d = 20; d <= 31; d++) {
            days.add(point(2026, 1, d, "1"));
        }

        List<MonthCell> cells = MonthMatrix.build(days, Granularity.DAY, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 31));

        assertEquals(1, cells.size());
        assertEquals(CellStatus.OK, cells.get(0).status());
    }

    @Test
    void invertedWindow_givesNoCells() {
        assertTrue(MonthMatrix.build(List.of(), Granularity.DAY, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1)).isEmpty());
    }

    @Test
    void noPoints_makeEveryMonthMissing() {
        List<MonthCell> cells = MonthMatrix.build(List.of(), Granularity.DAY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));

        assertEquals(2, cells.size());
        assertTrue(cells.stream().allMatch(c -> c.status() == CellStatus.MISSING));
    }
}
