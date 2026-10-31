-- Gün 5: hesap seviyesinde metrik serisi modeli
-- Gerçek biçimli örnek dosyalar (X aylık özet, Instagram günlük seri) gönderi bazlı değil
-- hesap seviyesinde olduğundan gönderi bazlı model kapsam dışına alındı.

-- Yükleme durumlarına RECEIVED eklenir (dosya okundu, henüz işlenmedi)
ALTER TABLE upload_batches DROP CONSTRAINT IF EXISTS upload_batches_status_check;
ALTER TABLE upload_batches
    ADD CONSTRAINT upload_batches_status_check
    CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED'));
ALTER TABLE upload_batches ALTER COLUMN status SET DEFAULT 'RECEIVED';

-- Dar (uzun) tablo: her ölçüm bir satır. Platformun vermediği metrik için satır yoktur,
-- böylece "0" ile "bilgi yok" birbirine karışmaz.
CREATE TABLE metric_points (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    batch_id     BIGINT        NOT NULL REFERENCES upload_batches(id) ON DELETE CASCADE,
    platform     VARCHAR(20)   NOT NULL CHECK (platform IN ('INSTAGRAM', 'X')),
    metric       VARCHAR(30)   NOT NULL,
    granularity  VARCHAR(10)   NOT NULL CHECK (granularity IN ('DAY', 'MONTH')),
    period_start DATE          NOT NULL,
    value        NUMERIC(18,2) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_metric_point UNIQUE (user_id, platform, metric, granularity, period_start)
);

CREATE INDEX idx_metric_points_series
    ON metric_points (user_id, platform, metric, granularity, period_start);
CREATE INDEX idx_metric_points_batch ON metric_points (batch_id);

-- Gönderi bazlı tablo kullanılmayacak
DROP TABLE IF EXISTS posts;
