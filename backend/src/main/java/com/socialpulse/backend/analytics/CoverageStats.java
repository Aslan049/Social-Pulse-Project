package com.socialpulse.backend.analytics;

import com.socialpulse.backend.entity.Granularity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Bir serinin kapsama göstergeleri. İki ayrı soruya cevap verir:
 * <ul>
 *   <li>Kendi aralığında boşluk var mı? (internalMissing: gerçek eksik veri)</li>
 *   <li>Analiz penceresinin ne kadarını kapsıyor? (windowCoverage; başta veya sonda eksik dönemler
 *       metriğin o tarihte başlamasından/bitmesinden kaynaklanabilir, eksik veri olmayabilir)</li>
 * </ul>
 */
public record CoverageStats(long ownExpected, long internalMissing, double ownCoverage,
                            long windowExpected, double windowCoverage,
                            long leadingMissing, long trailingMissing) {

    public static CoverageStats compute(Granularity granularity, LocalDate first, LocalDate last, long periods,
                                        LocalDate windowStart, LocalDate windowEnd) {
        long ownExpected = between(granularity, first, last) + 1;
        long windowExpected = between(granularity, windowStart, windowEnd) + 1;
        long leading = Math.max(0, between(granularity, windowStart, first));
        long trailing = Math.max(0, between(granularity, last, windowEnd));
        long internal = Math.max(0, ownExpected - periods);
        return new CoverageStats(
                ownExpected,
                internal,
                ratio(periods, ownExpected),
                windowExpected,
                ratio(periods, windowExpected),
                leading,
                trailing);
    }

    /** İki tarih arasındaki dönem farkı: günlükte gün, aylıkta ay. */
    public static long between(Granularity granularity, LocalDate from, LocalDate to) {
        return granularity == Granularity.DAY
                ? ChronoUnit.DAYS.between(from, to)
                : ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to));
    }

    private static double ratio(long observed, long expected) {
        if (expected <= 0) {
            return 0.0;
        }
        double value = Math.min(1.0, (double) observed / expected);
        return Math.round(value * 10_000d) / 10_000d;
    }
}
