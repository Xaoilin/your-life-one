-- One-time import of the calendar the React app kept in Supabase's public.yourlifeone_records
-- (collections calendarAccounts, calendarSources, calendarEvents), so moving to this service loses no
-- local accounts, calendars, colours or events. Google-backed events are also re-mirrored by the next sync.
-- A no-op where that table does not exist (tests, fresh databases). Rows that do not parse are skipped.
-- Temporary: once the React app reads calendar data only from this service, yourlifeone_records calendar rows
-- are no longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_date(value text) RETURNS date LANGUAGE plpgsql AS $$
BEGIN
    IF value !~ '^\d{4}-\d{2}-\d{2}$' THEN
        RETURN NULL;
    END IF;
    RETURN value::date;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO account (user_id, id, provider, name, email, is_primary, palette_index, auth_status,
                         auth_error, last_synced_at, sync_error)
    SELECT DISTINCT ON (r.user_id, CASE WHEN r.payload ->> 'provider' = 'google'
                                        THEN lower(r.payload ->> 'email') ELSE r.record_id END)
           r.user_id,
           r.record_id,
           CASE WHEN r.payload ->> 'provider' = 'google' THEN 'google' ELSE 'local' END,
           left(coalesce(nullif(btrim(r.payload ->> 'name'), ''), nullif(btrim(r.payload ->> 'email'), ''), 'Calendar'), 200),
           left(CASE WHEN r.payload ->> 'provider' = 'google' THEN lower(btrim(r.payload ->> 'email'))
                     ELSE coalesce(btrim(r.payload ->> 'email'), '') END, 320),
           coalesce((r.payload ->> 'isPrimary')::boolean, false),
           CASE WHEN r.payload ->> 'paletteIndex' ~ '^\d{1,3}$' THEN (r.payload ->> 'paletteIndex')::integer END,
           CASE WHEN r.payload ->> 'provider' = 'google'
                     AND r.payload ->> 'authStatus' IN ('connected', 'needs_reconnect', 'revoked', 'error')
                THEN r.payload ->> 'authStatus' ELSE 'connected' END,
           r.payload ->> 'lastAuthError',
           pg_temp.try_instant(r.payload ->> 'lastSyncTime'),
           r.payload ->> 'syncError'
    FROM public.yourlifeone_records r
    WHERE r.collection = 'calendarAccounts'
      AND r.deleted_at IS NULL
      AND coalesce((r.payload ->> 'mocked')::boolean, false) = false
      AND (r.payload ->> 'provider' <> 'google' OR coalesce(btrim(r.payload ->> 'email'), '') <> '')
    ORDER BY r.user_id, CASE WHEN r.payload ->> 'provider' = 'google'
                             THEN lower(r.payload ->> 'email') ELSE r.record_id END, r.updated_at DESC
    ON CONFLICT DO NOTHING;

    INSERT INTO source (user_id, id, account_id, name, color, visible, google_calendar_id, access_role)
    SELECT DISTINCT ON (r.user_id, r.payload ->> 'accountId', coalesce(r.payload ->> 'googleCalendarId', r.record_id))
           r.user_id,
           r.record_id,
           r.payload ->> 'accountId',
           left(coalesce(nullif(btrim(r.payload ->> 'name'), ''), 'Calendar'), 200),
           left(coalesce(nullif(btrim(r.payload ->> 'color'), ''), '#4f5bff'), 32),
           coalesce((r.payload ->> 'visible')::boolean, true),
           nullif(r.payload ->> 'googleCalendarId', ''),
           r.payload ->> 'accessRole'
    FROM public.yourlifeone_records r
    JOIN account a ON a.user_id = r.user_id AND a.id = r.payload ->> 'accountId'
    WHERE r.collection = 'calendarSources'
      AND r.deleted_at IS NULL
    ORDER BY r.user_id, r.payload ->> 'accountId', coalesce(r.payload ->> 'googleCalendarId', r.record_id),
             r.updated_at DESC
    ON CONFLICT DO NOTHING;

    INSERT INTO event (user_id, id, source_id, title, description, location, all_day, starts_at, ends_at,
                       google_event_id)
    SELECT DISTINCT ON (e.user_id, e.source_id, coalesce(e.google_event_id, e.id))
           e.user_id, e.id, e.source_id, e.title, e.description, e.location, e.all_day, e.starts_at, e.ends_at,
           e.google_event_id
    FROM (
        SELECT r.user_id,
               r.record_id AS id,
               r.payload ->> 'sourceId' AS source_id,
               left(coalesce(nullif(btrim(r.payload ->> 'title'), ''), '(No title)'), 500) AS title,
               coalesce(r.payload ->> 'description', '') AS description,
               nullif(r.payload ->> 'location', '') AS location,
               coalesce((r.payload ->> 'allDay')::boolean, false) AS all_day,
               CASE WHEN coalesce((r.payload ->> 'allDay')::boolean, false)
                    THEN pg_temp.try_date(r.payload ->> 'start')::timestamp AT TIME ZONE 'UTC'
                    ELSE pg_temp.try_instant(r.payload ->> 'start') END AS starts_at,
               CASE WHEN coalesce((r.payload ->> 'allDay')::boolean, false)
                    THEN (pg_temp.try_date(r.payload ->> 'end') + 1)::timestamp AT TIME ZONE 'UTC'
                    ELSE pg_temp.try_instant(r.payload ->> 'end') END AS ends_at,
               nullif(r.payload ->> 'googleEventId', '') AS google_event_id,
               r.updated_at
        FROM public.yourlifeone_records r
        WHERE r.collection = 'calendarEvents'
          AND r.deleted_at IS NULL
          AND coalesce(r.payload ->> 'pendingSync', '') <> 'delete'
    ) e
    JOIN source s ON s.user_id = e.user_id AND s.id = e.source_id
    WHERE e.starts_at IS NOT NULL AND e.ends_at IS NOT NULL AND e.ends_at >= e.starts_at
    ORDER BY e.user_id, e.source_id, coalesce(e.google_event_id, e.id), e.updated_at DESC
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
