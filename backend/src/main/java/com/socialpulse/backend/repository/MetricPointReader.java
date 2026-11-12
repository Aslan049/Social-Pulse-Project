package com.socialpulse.backend.repository;

import com.socialpulse.backend.analytics.StoredPoint;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * metric_points tablosundan okuma (analiz sorguları). Tüm sorgular kullanıcıya göre filtrelenir.
 * Negatif değerler veri hatası sayılır: noktalar ve özet istatistikler yalnızca negatif olmayan değerlerle
 * hesaplanır, negatiflerin sayısı ayrıca döner.
 */
@Repository
@RequiredArgsConstructor
public class MetricPointReader {

    private final JdbcTemplate jdbcTemplate;

    /** @param periods negatif olmayan değeri olan dönem sayısı; first/last de yalnızca bunlardan hesaplanır */
    public record SummaryRow(Platform platform, Metric metric, Granularity granularity, long periods,
                             long negativeCount, BigDecimal total, BigDecimal min, BigDecimal max,
                             LocalDate first, LocalDate last) {}

    public List<StoredPoint> findPoints(Long userId, Platform platform, Metric metric, Granularity granularity,
                                        LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                "SELECT period_start, value FROM metric_points "
                        + "WHERE user_id = ? AND platform = ? AND metric = ? AND granularity = ? AND value >= 0");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add(platform.name());
        args.add(metric.name());
        args.add(granularity.name());
        appendRange(sql, args, from, to);
        sql.append(" ORDER BY period_start");
        return jdbcTemplate.query(sql.toString(),
                (rs, i) -> new StoredPoint(rs.getDate("period_start").toLocalDate(), rs.getBigDecimal("value")),
                args.toArray());
    }

    public List<SummaryRow> summarize(Long userId, Platform platform, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                "SELECT platform, metric, granularity, "
                        + "COUNT(*) FILTER (WHERE value >= 0) AS periods, "
                        + "COUNT(*) FILTER (WHERE value < 0) AS negative_count, "
                        + "SUM(value) FILTER (WHERE value >= 0) AS total, "
                        + "MIN(value) FILTER (WHERE value >= 0) AS min_value, "
                        + "MAX(value) FILTER (WHERE value >= 0) AS max_value, "
                        + "MIN(period_start) FILTER (WHERE value >= 0) AS first_period, "
                        + "MAX(period_start) FILTER (WHERE value >= 0) AS last_period "
                        + "FROM metric_points WHERE user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (platform != null) {
            sql.append(" AND platform = ?");
            args.add(platform.name());
        }
        appendRange(sql, args, from, to);
        sql.append(" GROUP BY platform, metric, granularity "
                + "HAVING COUNT(*) FILTER (WHERE value >= 0) > 0 "
                + "ORDER BY platform, metric, granularity");
        return jdbcTemplate.query(sql.toString(),
                (rs, i) -> new SummaryRow(
                        Platform.valueOf(rs.getString("platform")),
                        Metric.valueOf(rs.getString("metric")),
                        Granularity.valueOf(rs.getString("granularity")),
                        rs.getLong("periods"),
                        rs.getLong("negative_count"),
                        rs.getBigDecimal("total"),
                        rs.getBigDecimal("min_value"),
                        rs.getBigDecimal("max_value"),
                        rs.getDate("first_period").toLocalDate(),
                        rs.getDate("last_period").toLocalDate()),
                args.toArray());
    }

    private static void appendRange(StringBuilder sql, List<Object> args, LocalDate from, LocalDate to) {
        if (from != null) {
            sql.append(" AND period_start >= ?");
            args.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND period_start <= ?");
            args.add(Date.valueOf(to));
        }
    }
}
