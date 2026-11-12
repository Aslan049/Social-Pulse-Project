package com.socialpulse.backend.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Günlük ölçümleri aylık toplamlara çevirir ve kapsama oranını hesaplar. */
public final class SeriesAggregator {

    private SeriesAggregator() {
    }

    /**
     * @param month        ayın ilk günü
     * @param total        aydaki gözlenen günlerin toplamı
     * @param observedDays veri bulunan gün sayısı
     * @param expectedDays olması gereken gün sayısı (ayın günleri, [from, to] aralığıyla kesişimi)
     */
    public record MonthValue(LocalDate month, BigDecimal total, int observedDays, int expectedDays) {
        public double coverage() {
            return expectedDays == 0 ? 0.0 : (double) observedDays / expectedDays;
        }
    }

    /**
     * Eksik günler toplamı sessizce küçültür, bu yüzden her ay için kapsama oranı da döner.
     * from/to boşsa ayın tamamı beklenir.
     */
    public static List<MonthValue> toMonths(List<StoredPoint> days, LocalDate from, LocalDate to) {
        Map<YearMonth, List<StoredPoint>> byMonth = new TreeMap<>();
        for (StoredPoint p : days) {
            byMonth.computeIfAbsent(YearMonth.from(p.periodStart()), k -> new ArrayList<>()).add(p);
        }
        List<MonthValue> result = new ArrayList<>();
        for (Map.Entry<YearMonth, List<StoredPoint>> entry : byMonth.entrySet()) {
            YearMonth month = entry.getKey();
            LocalDate first = month.atDay(1);
            LocalDate last = month.atEndOfMonth();
            if (from != null && from.isAfter(first)) {
                first = from;
            }
            if (to != null && to.isBefore(last)) {
                last = to;
            }
            int expected = last.isBefore(first) ? 0 : (int) (last.toEpochDay() - first.toEpochDay() + 1);
            BigDecimal total = entry.getValue().stream()
                    .map(StoredPoint::value)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int observed = (int) entry.getValue().stream().map(StoredPoint::periodStart).distinct().count();
            result.add(new MonthValue(month.atDay(1), total, observed, expected));
        }
        result.sort(Comparator.comparing(MonthValue::month));
        return result;
    }
}
