-- One-time import of the settings and integrations the React app kept in Supabase's public.yourlifeone_records
-- (collections settings and integrations), so moving to this service loses no preference or goal tag.
-- A no-op where that table does not exist (tests, fresh databases). Values that do not parse fall back
-- to the defaults the React app showed for them.
-- Temporary: once the React app reads these only from this service, yourlifeone_records rows for them are no
-- longer written and this migration never has anything new to import.
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

    INSERT INTO app_preferences (user_id, theme, data_retention_days, telemetry, default_calendar_tab, updated_at)
    SELECT DISTINCT ON (r.user_id)
           r.user_id,
           CASE WHEN r.payload ->> 'theme' IN ('dark', 'light') THEN r.payload ->> 'theme' ELSE 'dark' END,
           CASE WHEN r.payload ->> 'dataRetentionDays' ~ '^\d{1,4}$'
                THEN least(greatest((r.payload ->> 'dataRetentionDays')::integer, 7), 365) ELSE 90 END,
           coalesce(r.payload -> 'telemetry' = 'true'::jsonb, false),
           CASE WHEN r.payload ->> 'defaultCalendarTab' IN ('month', 'week', 'agenda', 'accounts')
                THEN r.payload ->> 'defaultCalendarTab' END,
           coalesce(r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'settings'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
    ORDER BY r.user_id, r.updated_at DESC
    ON CONFLICT DO NOTHING;

    -- Tags keep their order; blanks, repeats, over-long names and anything past 50 are dropped. The
    -- table is new, so there is nothing to conflict with.
    INSERT INTO goal_tag (user_id, position, name)
    SELECT ranked.user_id, ranked.position, ranked.name
    FROM (
        SELECT unique_tags.user_id, unique_tags.name,
               (row_number() OVER (PARTITION BY unique_tags.user_id ORDER BY unique_tags.first_index) - 1)::integer
                   AS position
        FROM (
            SELECT tags.user_id, tags.name, min(tags.index) AS first_index
            FROM (
                SELECT DISTINCT ON (r.user_id) r.user_id, r.payload -> 'goalTags' AS tags
                FROM public.yourlifeone_records r
                WHERE r.collection = 'settings'
                  AND r.deleted_at IS NULL
                  AND jsonb_typeof(r.payload -> 'goalTags') = 'array'
                ORDER BY r.user_id, r.updated_at DESC
            ) latest
            CROSS JOIN LATERAL (
                SELECT latest.user_id, btrim(tag.value) AS name, tag.index
                FROM jsonb_array_elements_text(latest.tags) WITH ORDINALITY AS tag(value, index)
            ) tags
            WHERE length(tags.name) BETWEEN 1 AND 60
            GROUP BY tags.user_id, tags.name
        ) unique_tags
    ) ranked
    JOIN app_preferences p ON p.user_id = ranked.user_id
    WHERE ranked.position < 50;

    INSERT INTO integration (user_id, provider, status, configured_at, last_error, updated_at)
    SELECT DISTINCT ON (r.user_id, r.payload ->> 'provider')
           r.user_id,
           r.payload ->> 'provider',
           CASE WHEN r.payload ->> 'status' IN ('connected', 'disconnected', 'error')
                THEN r.payload ->> 'status' ELSE 'disconnected' END,
           pg_temp.try_instant(r.payload ->> 'configuredAt'),
           left(nullif(btrim(r.payload ->> 'lastError'), ''), 500),
           coalesce(r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'integrations'
      AND r.deleted_at IS NULL
      AND r.payload ->> 'provider' = 'google'
    ORDER BY r.user_id, r.payload ->> 'provider', r.updated_at DESC
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove it now.
DROP FUNCTION pg_temp.try_instant(text);
