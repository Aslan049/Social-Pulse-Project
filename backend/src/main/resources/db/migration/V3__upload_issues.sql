-- Gün 9: yükleme sorunları (hata ve uyarı) kalıcı hale gelir
ALTER TABLE upload_batches ADD COLUMN warning_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE upload_errors ADD COLUMN sheet VARCHAR(100);
ALTER TABLE upload_errors
    ADD COLUMN severity VARCHAR(10) NOT NULL DEFAULT 'ERROR'
    CHECK (severity IN ('ERROR', 'WARNING'));
