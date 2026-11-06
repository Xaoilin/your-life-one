-- One-time import of the equity positions the React app kept in Supabase's public.yourlifeone_records (collection
-- equityPositions, record_id = position id; deleted rows are skipped). A no-op where that table does not
-- exist (tests, fresh databases). Each position keeps its document as saved; identity and timestamps
-- become columns.
-- Temporary: once the React app reads equity only from this service, yourlifeone_records rows for it are no
-- longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_date(value text) RETURNS date LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN value ~ '^\d{4}-\d{2}-\d{2}$' THEN value::date END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO position (user_id, id, document, company, as_of, created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           r.payload - 'id' - 'createdAt' - 'updatedAt',
           coalesce(nullif(left(btrim(r.payload ->> 'company'), 200), ''), 'Unknown company'),
           coalesce(pg_temp.try_date(r.payload ->> 'asOf'), (coalesce(r.updated_at, now()) AT TIME ZONE 'UTC')::date),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'equityPositions' AND r.deleted_at IS NULL AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
