-- One-time import of the projects and wiki pages the React app kept in Supabase's public.yourlifeone_records
-- (collections projects and projectPages; the older "workspaces" collection is empty everywhere).
-- A no-op where that table does not exist (tests, fresh databases). The app saved projects already
-- normalised, so values are copied with the same fallbacks it used when reading them.
-- Temporary: once the React app reads projects only from this service, yourlifeone_records rows for them are
-- no longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.object_array(value jsonb, max_items integer) RETURNS jsonb LANGUAGE sql AS $$
    SELECT CASE WHEN jsonb_typeof(value) = 'array'
                THEN (SELECT coalesce(jsonb_agg(element ORDER BY position), '[]')
                      FROM jsonb_array_elements(value) WITH ORDINALITY AS listed(element, position)
                      WHERE jsonb_typeof(element) = 'object' AND position <= max_items)
                ELSE '[]' END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO project (user_id, id, catalog_key, name, kind, summary, status, status_before_archive, is_pinned,
                         sort_order, tags, links, setup_steps, run_recipes, preview, verified_at, created_at,
                         updated_at)
    SELECT r.user_id,
           r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'catalogKey'), 300), ''), 'custom:' || r.record_id),
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 160), ''), 'Project'),
           CASE WHEN r.payload ->> 'kind' IN ('web_app', 'desktop_app', 'mobile_app', 'cli', 'service', 'library',
                                               'automation', 'hardware', 'research', 'other')
                THEN r.payload ->> 'kind' ELSE 'other' END,
           left(btrim(coalesce(r.payload ->> 'summary', '')), 4000),
           status,
           CASE WHEN status = 'archived' AND r.payload ->> 'statusBeforeArchive' IN ('planning', 'active', 'blocked', 'completed')
                THEN r.payload ->> 'statusBeforeArchive' END,
           status <> 'archived' AND coalesce((r.payload ->> 'isPinned') = 'true', false),
           CASE WHEN jsonb_typeof(r.payload -> 'sortOrder') = 'number' AND (r.payload ->> 'sortOrder') ~ '^[0-9]{1,9}$'
                THEN (r.payload ->> 'sortOrder')::integer ELSE 0 END,
           coalesce(ARRAY(SELECT DISTINCT btrim(tag)
                          FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'tags') = 'array'
                                                              THEN r.payload -> 'tags' ELSE '[]' END) tag
                          WHERE btrim(tag) <> ''
                          LIMIT 25), '{}'),
           pg_temp.object_array(r.payload -> 'links', 50),
           pg_temp.object_array(r.payload -> 'setupSteps', 50),
           pg_temp.object_array(r.payload -> 'runRecipes', 20),
           CASE WHEN jsonb_typeof(r.payload -> 'preview') = 'object' THEN r.payload -> 'preview'
                ELSE '{"icon": "folder", "accentColor": "#7c6cff", "backgroundColor": "#171827"}' END,
           pg_temp.try_instant(r.payload ->> 'verifiedAt'),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    CROSS JOIN LATERAL (SELECT CASE WHEN r.payload ->> 'status' IN ('planning', 'active', 'blocked', 'completed', 'archived')
                                    THEN r.payload ->> 'status' ELSE 'active' END AS status) normalised
    WHERE r.collection = 'projects'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;

    -- Pages of projects that are gone were invisible in the app; they are left behind. Where a project had
    -- several overview pages, the first created stays the overview.
    INSERT INTO page (user_id, id, project_id, title, content, is_overview, created_at, updated_at)
    SELECT user_id, id, project_id, title, content,
           is_overview AND row_number() OVER (PARTITION BY user_id, project_id, is_overview
                                              ORDER BY created_at, id) = 1,
           created_at, updated_at
    FROM (SELECT r.user_id,
                 r.record_id AS id,
                 r.payload ->> 'projectId' AS project_id,
                 coalesce(nullif(left(btrim(r.payload ->> 'title'), 200), ''), 'Untitled Page') AS title,
                 left(coalesce(r.payload ->> 'content', ''), 200000) AS content,
                 coalesce((r.payload ->> 'isOverview') = 'true', false) AS is_overview,
                 coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()) AS created_at,
                 coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now()) AS updated_at
          FROM public.yourlifeone_records r
          JOIN project p ON p.user_id = r.user_id AND p.id = r.payload ->> 'projectId'
          WHERE r.collection = 'projectPages'
            AND r.deleted_at IS NULL
            AND jsonb_typeof(r.payload) = 'object'
            AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$') pages
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.object_array(jsonb, integer);
