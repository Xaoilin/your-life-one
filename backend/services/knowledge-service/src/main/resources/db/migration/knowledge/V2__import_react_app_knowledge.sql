-- One-time import of the knowledge base the React app kept in Supabase's public.yourlifeone_records
-- (collections knowledgeTopics and knowledgeEntries). A no-op where that table does not exist (tests,
-- fresh databases). Every live record is kept: unreadable values fall back to what the app showed.
-- Temporary: once the React app reads the knowledge base only from this service, yourlifeone_records rows for
-- it are no longer written and this migration never has anything new to import.
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

    INSERT INTO topic (user_id, id, name, description, icon, color, sort_order, created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 160), ''), 'Untitled topic'),
           left(coalesce(r.payload ->> 'description', ''), 4000),
           left(coalesce(r.payload ->> 'icon', ''), 40),
           left(coalesce(r.payload ->> 'color', ''), 32),
           CASE WHEN jsonb_typeof(r.payload -> 'sortOrder') = 'number' AND (r.payload ->> 'sortOrder') ~ '^[0-9]{1,9}$'
                THEN (r.payload ->> 'sortOrder')::integer ELSE 0 END,
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'knowledgeTopics'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;

    -- Entries whose topic is gone were invisible in the app; they are left behind.
    INSERT INTO entry (user_id, id, topic_id, title, content, sources, tags, created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           r.payload ->> 'topicId',
           coalesce(nullif(left(btrim(r.payload ->> 'title'), 300), ''), 'Untitled entry'),
           left(coalesce(r.payload ->> 'content', ''), 100000),
           CASE WHEN jsonb_typeof(r.payload -> 'sources') = 'array' AND jsonb_array_length(r.payload -> 'sources') <= 50
                THEN (SELECT coalesce(jsonb_agg(source), '[]')
                      FROM jsonb_array_elements(r.payload -> 'sources') source
                      WHERE jsonb_typeof(source) = 'object')
                ELSE '[]' END,
           coalesce(ARRAY(SELECT DISTINCT left(btrim(tag), 50)
                          FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'tags') = 'array'
                                                              THEN r.payload -> 'tags' ELSE '[]' END) tag
                          WHERE btrim(tag) <> ''
                          LIMIT 25), '{}'),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    JOIN topic t ON t.user_id = r.user_id AND t.id = r.payload ->> 'topicId'
    WHERE r.collection = 'knowledgeEntries'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
