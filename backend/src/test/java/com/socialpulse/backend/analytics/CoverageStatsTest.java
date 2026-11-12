package com.socialpulse.backend.analytics;

import com.socialpulse.backend.entity.Granularity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CoverageStatsTest {

    @Test
    void dailySeriesStartingLate_hasFullOwnCoverageButPartialWindowCoverage() {
        CoverageStats stats = CoverageStats.compute(Granularity.DAY,
                LocalDate.of(2025, 10, 15), LocalDate.of(2026, 8, 31), 321,
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31));

        assertEquals(321, stats.ownExpected());
        assertEquals(0, stats.internalMissing());
        assertEquals(1.0, stats.ownCoverage());
        assertEquals(365, stats.windowExpected());
        assertEquals(0.8795, stats.windowCoverage(), 1e-9);
        assertEquals(44, stats.leadingMissing());
        assertEquals(0, stats.trailingMissing());
    }

    @Test
    void monthlySeriesMissingTheWindowsFirstMonth_isNotFullyCoveredInTheWindow() {
        CoverageStats stats = CoverageStats.compute(Granularity.MONTH,
                LocalDate.of(2025, 10, 1), LocalDate.of(2026, 8, 1), 11,
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31));

        assertEquals(11, stats.ownExpected());
        assertEquals(1.0, stats.ownCoverage());
        assertEquals(12, stats.windowExpected());
        assertEquals(0.9167, stats.windowCoverage(), 1e-9);
        assertEquals(1, stats.leadingMissing());
    }

    @Test
    void gapsInsideTheOwnRange_areCountedAsInternalMissing() {
        CoverageStats stats = CoverageStats.compute(Granularity.MONTH,
                LocalDate.of(2025, 10, 1), LocalDate.of(2026, 8, 1), 9,
                LocalDate.of(2025, 10, 1), LocalDate.of(2026, 8, 31));

        assertEquals(2, stats.internalMissing());
        assertEquals(0, stats.leadingMissing());
        assertEquals(0.8182, stats.ownCoverage(), 1e-9);
    }

    @Test
    void trailingMissing_isCounted() {
        CoverageStats stats = CoverageStats.compute(Granularity.DAY,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20), 20,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(11, stats.trailingMissing());
        assertEquals(0, stats.leadingMissing());
    }
}
