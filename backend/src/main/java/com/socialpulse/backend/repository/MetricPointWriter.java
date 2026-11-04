package com.socialpulse.backend.repository;

import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.ingestion.ParsedPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

/**
 * metric_points tablosuna toplu "upsert" yazar. Aynı (kullanıcı, platform, metrik, ayrıntı, dönem)
 * tekrar yüklenirse değer ve batch güncellenir, çift kayıt oluşmaz.
 */
@Repository
@RequiredArgsConstructor
public class MetricPointWriter {

    private static final String UPSERT_SQL = """
            INSERT INTO metric_points (user_id, batch_id, platform, metric, granularity, period_start, value)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, platform, metric, granularity, period_start)
            DO UPDATE SET value = EXCLUDED.value, batch_id = EXCLUDED.batch_id
            """;

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    /** Yazılan (eklenen veya güncellenen) nokta sayısını döner. */
    public int upsertAll(Long userId, Long batchId, Platform platform, List<ParsedPoint> points) {
        if (points.isEmpty()) {
            return 0;
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, points, BATCH_SIZE, (ps, point) -> {
            ps.setLong(1, userId);
            ps.setLong(2, batchId);
            ps.setString(3, platform.name());
            ps.setString(4, point.metric().name());
            ps.setString(5, point.granularity().name());
            ps.setDate(6, Date.valueOf(point.periodStart()));
            ps.setBigDecimal(7, point.value());
        });
        return points.size();
    }
}
