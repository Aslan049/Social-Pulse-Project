package com.socialpulse.backend.quality;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.IssueSeverity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.ingestion.ParsedPoint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Yükleme sırasında ölçümler üzerinde yapılan basit sağlık kontrolleri. Veriyi reddetmez, bulgu üretir.
 * <ul>
 *   <li>WARNING, negatif değer (veri hatası olabilir; özet istatistiklere dahil edilmez)</li>
 *   <li>WARNING, iç boşluk: bir metriğin kendi ilk-son aralığı içinde eksik dönemler (gerçek eksik veri)</li>
 *   <li>INFO, bir metrik ortak aralığın ortasında başlıyor/bitiyor (metrik o tarihte başlamış olabilir,
 *       eksik veri sayılmaz)</li>
 *   <li>WARNING, aşırı uç değer: medyanın 100 katından büyük (en az 10 noktalı seride)</li>
 * </ul>
 * İstatistiksel anormal gün tespiti ayrı bir analizdir.
 */
@Component
public class DataQualityChecker {

    static final BigDecimal EXTREME_FACTOR = new BigDecimal("100");
    static final int MIN_POINTS_FOR_EXTREME = 10;
    static final int MAX_FINDINGS_PER_KIND = 20;

    public record QualityFinding(Metric metric, IssueSeverity severity, String message) {}

    public List<QualityFinding> check(List<ParsedPoint> points) {
        List<QualityFinding> findings = new ArrayList<>();
        if (points.isEmpty()) {
            return findings;
        }
        Map<Metric, List<ParsedPoint>> byMetric = new EnumMap<>(Metric.class);
        for (ParsedPoint p : points) {
            byMetric.computeIfAbsent(p.metric(), k -> new ArrayList<>()).add(p);
        }
        byMetric.values().forEach(list -> list.sort(Comparator.comparing(ParsedPoint::periodStart)));

        negativeValues(byMetric, findings);
        availability(points, findings);
        extremeValues(byMetric, findings);
        return findings;
    }

    private void negativeValues(Map<Metric, List<ParsedPoint>> byMetric, List<QualityFinding> out) {
        int reported = 0;
        int total = 0;
        for (Map.Entry<Metric, List<ParsedPoint>> entry : byMetric.entrySet()) {
            for (ParsedPoint p : entry.getValue()) {
                if (p.value().signum() < 0) {
                    total++;
                    if (reported < MAX_FINDINGS_PER_KIND) {
                        reported++;
                        out.add(new QualityFinding(p.metric(), IssueSeverity.WARNING, "Negatif değer: "
                                + p.periodStart() + " = " + p.value().stripTrailingZeros().toPlainString()
                                + ". Veri hatası olabilir, özet istatistiklere dahil edilmez."));
                    }
                }
            }
        }
        if (total > reported) {
            out.add(new QualityFinding(null, IssueSeverity.WARNING, "… ve " + (total - reported) + " negatif değer daha"));
        }
    }

    /**
     * Her çözünürlük için ortak aralık (noktaların en küçük ve en büyük dönemi) bulunur. Bir metriğin kendi
     * aralığı içindeki boşluklar WARNING, ortak aralığın başında/sonunda veri olmaması INFO olur. Negatif
     * değerli dönemler "var" sayılır: onlar zaten ayrı bir uyarıyla bildirilir, aynı günü iki kez bildirmemek için.
     */
    private void availability(List<ParsedPoint> all, List<QualityFinding> out) {
        for (Granularity granularity : Granularity.values()) {
            List<ParsedPoint> ofGranularity = all.stream().filter(p -> p.granularity() == granularity).toList();
            if (ofGranularity.isEmpty()) {
                continue;
            }
            LocalDate windowStart = ofGranularity.stream().map(ParsedPoint::periodStart).min(Comparator.naturalOrder()).orElseThrow();
            LocalDate windowEnd = ofGranularity.stream().map(ParsedPoint::periodStart).max(Comparator.naturalOrder()).orElseThrow();
            String unit = granularity == Granularity.DAY ? "gün" : "ay";

            Map<Metric, TreeSet<LocalDate>> datesByMetric = new EnumMap<>(Metric.class);
            for (ParsedPoint p : ofGranularity) {
                datesByMetric.computeIfAbsent(p.metric(), k -> new TreeSet<>()).add(p.periodStart());
            }

            for (Map.Entry<Metric, TreeSet<LocalDate>> entry : datesByMetric.entrySet()) {
                LocalDate first = entry.getValue().first();
                LocalDate last = entry.getValue().last();
                long observed = entry.getValue().size();
                long ownExpected = between(granularity, first, last) + 1;

                if (observed < ownExpected) {
                    out.add(new QualityFinding(entry.getKey(), IssueSeverity.WARNING,
                            (ownExpected - observed) + " " + unit + " için veri yok (kendi aralığı içinde: "
                                    + first + " – " + last + ", " + observed + "/" + ownExpected + " " + unit
                                    + "). Eksik dönemler 0 sayılmaz."));
                }
                long leading = between(granularity, windowStart, first);
                if (leading > 0) {
                    out.add(new QualityFinding(entry.getKey(), IssueSeverity.INFO,
                            "Metrik " + first + " tarihinde başlıyor; bu yüklemedeki ortak aralığın ilk " + leading
                                    + " " + unit + " için veri yok. Metrik o tarihten itibaren mevcut olabilir, "
                                    + "eksik veri sayılmadı."));
                }
                long trailing = between(granularity, last, windowEnd);
                if (trailing > 0) {
                    out.add(new QualityFinding(entry.getKey(), IssueSeverity.INFO,
                            "Metrik " + last + " tarihinde bitiyor; ortak aralığın son " + trailing + " " + unit
                                    + " için veri yok. Eksik veri sayılmadı."));
                }
            }
        }
    }

    private static long between(Granularity granularity, LocalDate from, LocalDate to) {
        return granularity == Granularity.DAY
                ? ChronoUnit.DAYS.between(from, to)
                : ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to));
    }

    private void extremeValues(Map<Metric, List<ParsedPoint>> byMetric, List<QualityFinding> out) {
        for (Map.Entry<Metric, List<ParsedPoint>> entry : byMetric.entrySet()) {
            List<ParsedPoint> series = entry.getValue().stream().filter(p -> p.value().signum() >= 0).toList();
            if (series.size() < MIN_POINTS_FOR_EXTREME) {
                continue;
            }
            BigDecimal median = median(series.stream().map(ParsedPoint::value).sorted().toList());
            if (median.signum() <= 0) {
                continue;
            }
            BigDecimal limit = median.multiply(EXTREME_FACTOR);
            int reported = 0;
            int total = 0;
            for (ParsedPoint p : series) {
                if (p.value().compareTo(limit) > 0) {
                    total++;
                    if (reported < MAX_FINDINGS_PER_KIND) {
                        reported++;
                        out.add(new QualityFinding(entry.getKey(), IssueSeverity.WARNING, "Aşırı uç değer: "
                                + p.periodStart() + " = " + p.value().stripTrailingZeros().toPlainString()
                                + " (medyanın yaklaşık "
                                + p.value().divide(median, 0, RoundingMode.HALF_UP).toPlainString() + " katı)."));
                    }
                }
            }
            if (total > reported) {
                out.add(new QualityFinding(entry.getKey(), IssueSeverity.WARNING,
                        "… ve " + (total - reported) + " aşırı uç değer daha"));
            }
        }
    }

    static BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }
}
