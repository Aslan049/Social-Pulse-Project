package com.socialpulse.backend.analytics;

import com.socialpulse.backend.analytics.SeriesAggregator.MonthValue;
import com.socialpulse.backend.dto.CellStatus;
import com.socialpulse.backend.dto.MonthCell;
import com.socialpulse.backend.entity.Granularity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bir seriyi pencere içindeki her ay için bir hücreye çevirir. Veri olmayan ay MISSING olur (0 değildir),
 * bazı günleri eksik ay PARTIAL olur.
 */
public final class MonthMatrix {

    private MonthMatrix() {
    }

    public static List<MonthCell> build(List<StoredPoint> points, Granularity source,
                                        LocalDate windowStart, LocalDate windowEnd) {
        List<MonthCell> cells = new ArrayList<>();
        if (windowEnd.isBefore(windowStart)) {
            return cells;
        }
        YearMonth start = YearMonth.from(windowStart);
        YearMonth end = YearMonth.from(windowEnd);

        Map<YearMonth, BigDecimal> monthlyValues = new HashMap<>();
        Map<YearMonth, MonthValue> aggregated = new HashMap<>();
        if (source == Granularity.MONTH) {
            for (StoredPoint p : points) {
                monthlyValues.put(YearMonth.from(p.periodStart()), p.value());
            }
        } else {
            for (MonthValue m : SeriesAggregator.toMonths(points, windowStart, windowEnd)) {
                aggregated.put(YearMonth.from(m.month()), m);
            }
        }

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            LocalDate month = ym.atDay(1);
            if (source == Granularity.MONTH) {
                BigDecimal value = monthlyValues.get(ym);
                cells.add(value == null
                        ? new MonthCell(month, null, null, CellStatus.MISSING)
                        : new MonthCell(month, value, null, CellStatus.OK));
            } else {
                MonthValue m = aggregated.get(ym);
                if (m == null) {
                    cells.add(new MonthCell(month, null, null, CellStatus.MISSING));
                } else {
                    double coverage = Math.round(Math.min(1.0, m.coverage()) * 10_000d) / 10_000d;
                    cells.add(new MonthCell(month, m.total(), coverage,
                            coverage < 1.0 ? CellStatus.PARTIAL : CellStatus.OK));
                }
            }
        }
        return cells;
    }
}
