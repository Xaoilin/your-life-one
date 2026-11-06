-- One-time import of the lifestyle tracker the React app kept in Supabase's public.yourlifeone_records
-- (collection lifestyleItems). A no-op where that table does not exist (tests, fresh databases).
-- Older items kept one reference in "source"; it becomes the first of "sources". Items without an
-- order take position 0, as the app showed them.
-- Temporary: once the React app reads the tracker only from this service, yourlifeone_records rows for it are
-- no longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO item (user_id, id, type, title, notes, status, sources, sort_order, created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           CASE WHEN r.payload ->> 'type' IN ('haram', 'major-sin', 'wajib-both', 'wajib-women', 'wajib-men', 'halal')
                THEN r.payload ->> 'type' ELSE 'haram' END,
           coalesce(nullif(left(btrim(r.payload ->> 'title'), 300), ''), 'Untitled item'),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           CASE WHEN r.payload ->> 'status' IN ('struggling', 'working-on-it', 'avoiding', 'mastered', 'want-to-start',
                                                 'sometimes', 'practicing', 'consistent')
                THEN r.payload ->> 'status'
                WHEN r.payload ->> 'type' IN ('wajib-both', 'wajib-women', 'wajib-men', 'halal') THEN 'want-to-start'
                ELSE 'struggling' END,
           coalesce(ARRAY(SELECT source FROM (
                              SELECT btrim(value) AS source, ordinality
                              FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'sources') = 'array'
                                                                  THEN r.payload -> 'sources' ELSE '[]' END)
                                   WITH ORDINALITY AS listed(value, ordinality)
                              UNION ALL
                              SELECT btrim(r.payload ->> 'source'), 0
                              WHERE jsonb_typeof(r.payload -> 'source') = 'string') sources
                          WHERE source <> ''
                          ORDER BY ordinality
                          LIMIT 25), '{}'),
           CASE WHEN jsonb_typeof(r.payload -> 'sortOrder') = 'number' AND (r.payload ->> 'sortOrder') ~ '^[0-9]{1,9}$'
                THEN (r.payload ->> 'sortOrder')::integer ELSE 0 END,
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'lifestyleItems'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
