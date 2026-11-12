-- Gün 13: veri kalitesi bulgularına INFO (bilgi) düzeyi eklenir.
-- Bir metriğin ortak aralığın ortasında başlaması eksik veri değil, bilgi notudur.
DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'upload_errors'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%severity%'
    LOOP
        EXECUTE format('ALTER TABLE upload_errors DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE upload_errors
    ADD CONSTRAINT upload_errors_severity_check
    CHECK (severity IN ('ERROR', 'WARNING', 'INFO'));
