package com.socialpulse.backend.service;

import com.socialpulse.backend.analytics.CoverageStats;
import com.socialpulse.backend.analytics.GrowthCalculator;
import com.socialpulse.backend.analytics.MonthMatrix;
import com.socialpulse.backend.analytics.SeriesAggregator;
import com.socialpulse.backend.analytics.StoredPoint;
import com.socialpulse.backend.dto.CellStatus;
import com.socialpulse.backend.dto.MetricComparison;
import com.socialpulse.backend.dto.MetricSummary;
import com.socialpulse.backend.dto.MonthCell;
import com.socialpulse.backend.dto.MonthlyChangeResponse;
import com.socialpulse.backend.dto.MonthlyChangeRow;
import com.socialpulse.backend.dto.PlatformComparison;
import com.socialpulse.backend.dto.PlatformSeries;
import com.socialpulse.backend.dto.SeriesPointDto;
import com.socialpulse.backend.dto.SeriesResponse;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.exception.InvalidRequestException;
import com.socialpulse.backend.repository.MetricPointReader;
import com.socialpulse.backend.repository.MetricPointReader.SummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MetricPointReader reader;

    /**
     * Bir metriğin zaman serisi. AY isteğinde platformun kendi aylık değerleri varsa onlar, yoksa günlük
     * değerler aya toplanır ve her ay için kapsama oranı verilir (eksik günler toplamı küçültür).
     */
    @Transactional(readOnly = true)
    public SeriesResponse series(Long userId, Platform platform, Metric metric, Granularity granularity,
                                 LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<SeriesPointDto> points;
        if (granularity == Granularity.DAY) {
            points = toDtos(reader.findPoints(userId, platform, metric, Granularity.DAY, from, to));
        } else {
            List<StoredPoint> stored = reader.findPoints(userId, platform, metric, Granularity.MONTH, from, to);
            if (!stored.isEmpty()) {
                points = toDtos(stored);
            } else {
                List<StoredPoint> days = reader.findPoints(userId, platform, metric, Granularity.DAY, from, to);
                points = SeriesAggregator.toMonths(days, from, to).stream()
                        .map(m -> new SeriesPointDto(m.month(), m.total(), round(m.coverage())))
                        .toList();
            }
        }
        return new SeriesResponse(platform, metric, granularity, points);
    }

    /**
     * Kullanıcının tüm (platform, metrik) çiftleri için özet. Her seri iki ayrı kapsamayla değerlendirilir:
     * kendi aralığında (gerçek boşluk var mı) ve analiz penceresinde (tüm platformların aralığının birleşimi;
     * başta veya sonda eksik dönemler metriğin o tarihte başlamasından kaynaklanabilir).
     */
    @Transactional(readOnly = true)
    public List<MetricSummary> summary(Long userId, Platform platform, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<SummaryRow> rows = reader.summarize(userId, platform, from, to);
        if (rows.isEmpty()) {
            return List.of();
        }
        LocalDate windowStart = windowStart(rows, from);
        LocalDate windowEnd = windowEnd(rows, to);
        return rows.stream().map(row -> toSummary(row, windowStart, windowEnd)).toList();
    }

    /**
     * Platformlar arası aylık karşılaştırma: en az iki platformda verisi olan metrikler için, platformların
     * aralığının birleşimi olan pencerede her ay bir hücre (tam, kısmi veya veri yok).
     */
    @Transactional(readOnly = true)
    public PlatformComparison platformComparison(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<SummaryRow> rows = reader.summarize(userId, null, from, to);

        Map<Metric, Map<Platform, SummaryRow>> byMetric = groupByMetric(rows);
        List<Metric> common = byMetric.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .toList();
        if (common.isEmpty()) {
            return new PlatformComparison(null, null, List.of(), List.of());
        }

        List<SummaryRow> commonRows = common.stream().flatMap(m -> byMetric.get(m).values().stream()).toList();
        LocalDate windowStart = windowStart(commonRows, from);
        LocalDate windowEnd = windowEnd(commonRows, to);

        List<LocalDate> months = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(windowStart); !ym.isAfter(YearMonth.from(windowEnd)); ym = ym.plusMonths(1)) {
            months.add(ym.atDay(1));
        }

        List<MetricComparison> metrics = new ArrayList<>();
        for (Metric metric : common) {
            List<PlatformSeries> platforms = new ArrayList<>();
            for (Map.Entry<Platform, SummaryRow> entry : byMetric.get(metric).entrySet()) {
                SummaryRow row = entry.getValue();
                List<StoredPoint> points = reader.findPoints(userId, entry.getKey(), metric, row.granularity(), from, to);
                List<MonthCell> cells = MonthMatrix.build(points, row.granularity(), windowStart, windowEnd);
                List<LocalDate> missing = cells.stream().filter(c -> c.status() == CellStatus.MISSING).map(MonthCell::month).toList();
                List<LocalDate> partial = cells.stream().filter(c -> c.status() == CellStatus.PARTIAL).map(MonthCell::month).toList();
                platforms.add(new PlatformSeries(entry.getKey(), row.granularity(), cells,
                        cells.size() - missing.size(), missing, partial));
            }
            metrics.add(new MetricComparison(metric, platforms));
        }
        return new PlatformComparison(windowStart, windowEnd, months, metrics);
    }

    /**
     * Bir (platform, metrik) için aylık değer ve önceki aya göre yüzde değişim. Önceki ay yoksa, 0 ise ya da
     * iki aydan biri kısmi ise yüzde ya hiç verilmez ya da "comparable=false" ve açıklamayla verilir.
     */
    @Transactional(readOnly = true)
    public MonthlyChangeResponse monthlyChange(Long userId, Platform platform, Metric metric,
                                               LocalDate from, LocalDate to) {
        validateRange(from, to);
        SummaryRow row = groupByMetric(reader.summarize(userId, platform, from, to))
                .getOrDefault(metric, Map.of()).get(platform);
        if (row == null) {
            return new MonthlyChangeResponse(platform, metric, null, null, null, List.of());
        }
        LocalDate windowStart = from != null ? from : row.first().withDayOfMonth(1);
        LocalDate windowEnd = to != null ? to : YearMonth.from(row.last()).atEndOfMonth();

        List<StoredPoint> points = reader.findPoints(userId, platform, metric, row.granularity(), from, to);
        List<MonthCell> cells = MonthMatrix.build(points, row.granularity(), windowStart, windowEnd);

        List<MonthlyChangeRow> result = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            MonthCell cell = cells.get(i);
            MonthCell previous = i > 0 ? cells.get(i - 1) : null;
            BigDecimal previousValue = previous == null ? null : previous.value();
            BigDecimal change = GrowthCalculator.percentChange(cell.value(), previousValue);
            boolean comparable = change != null && cell.status() == CellStatus.OK && previous.status() == CellStatus.OK;
            result.add(new MonthlyChangeRow(cell.month(), cell.value(), cell.coverage(), cell.status(),
                    previousValue, change, comparable, note(cell, previous)));
        }
        return new MonthlyChangeResponse(platform, metric, row.granularity(), windowStart, windowEnd, result);
    }

    private static String note(MonthCell cell, MonthCell previous) {
        if (previous == null) {
            return null;
        }
        if (cell.status() == CellStatus.MISSING) {
            return "Bu ay için veri yok";
        }
        if (previous.status() == CellStatus.MISSING) {
            return "Önceki ay için veri yok";
        }
        if (previous.value().signum() == 0) {
            return "Önceki ay 0 olduğundan yüzde hesaplanamaz";
        }
        if (cell.status() == CellStatus.PARTIAL || previous.status() == CellStatus.PARTIAL) {
            return "Kapsam eksik: yüzde değişim yanıltıcı olabilir";
        }
        return null;
    }

    /** Aynı (metrik, platform) için hem günlük hem aylık satır varsa günlük (daha ayrıntılı) tercih edilir. */
    private static Map<Metric, Map<Platform, SummaryRow>> groupByMetric(List<SummaryRow> rows) {
        Map<Metric, Map<Platform, SummaryRow>> byMetric = new EnumMap<>(Metric.class);
        for (SummaryRow row : rows) {
            byMetric.computeIfAbsent(row.metric(), k -> new EnumMap<>(Platform.class))
                    .merge(row.platform(), row, (a, b) -> a.granularity() == Granularity.DAY ? a : b);
        }
        return byMetric;
    }

    private static LocalDate windowStart(List<SummaryRow> rows, LocalDate from) {
        if (from != null) {
            return from;
        }
        return rows.stream().map(SummaryRow::first).min(Comparator.naturalOrder()).orElseThrow().withDayOfMonth(1);
    }

    private static LocalDate windowEnd(List<SummaryRow> rows, LocalDate to) {
        if (to != null) {
            return to;
        }
        return YearMonth.from(rows.stream().map(SummaryRow::last).max(Comparator.naturalOrder()).orElseThrow()).atEndOfMonth();
    }

    private static MetricSummary toSummary(SummaryRow row, LocalDate windowStart, LocalDate windowEnd) {
        BigDecimal average = row.total().divide(BigDecimal.valueOf(row.periods()), 2, RoundingMode.HALF_UP);
        CoverageStats stats = CoverageStats.compute(
                row.granularity(), row.first(), row.last(), row.periods(), windowStart, windowEnd);
        return new MetricSummary(row.platform(), row.metric(), row.granularity(), row.periods(),
                row.total(), average, row.min(), row.max(), row.first(), row.last(),
                stats.ownCoverage(), stats.internalMissing(),
                windowStart, windowEnd, stats.windowCoverage(),
                stats.leadingMissing(), stats.trailingMissing(), row.negativeCount());
    }

    private static List<SeriesPointDto> toDtos(List<StoredPoint> points) {
        return points.stream().map(p -> new SeriesPointDto(p.periodStart(), p.value(), null)).toList();
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("Başlangıç tarihi bitiş tarihinden sonra olamaz");
        }
    }
}
