-- One-time import of the clock workspace the React app kept in Supabase's public.yourlifeone_records (collection
-- clock): the counters (record meta), stopwatches and timers. A no-op where that table does not exist.
-- A running timer without its end, or a paused one, is imported idle, as the app showed it.
-- Temporary: once the React app reads the clock only from this service, yourlifeone_records rows for it are no
-- longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.whole(value jsonb) RETURNS bigint LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN jsonb_typeof(value) = 'number' AND (value #>> '{}') ~ '^[0-9]{1,15}(\.0+)?$'
                THEN floor((value #>> '{}')::numeric)::bigint END
$$;

CREATE OR REPLACE FUNCTION pg_temp.label(value text, fallback text) RETURNS text LANGUAGE sql IMMUTABLE AS $$
    SELECT coalesce(nullif(left(btrim(value), 100), ''), fallback)
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO counter (user_id, next_stopwatch_number, next_timer_number)
    SELECT r.user_id,
           greatest(1, least(coalesce(pg_temp.whole(r.payload -> 'nextStopwatchNumber'), 1), 1000000))::integer,
           greatest(1, least(coalesce(pg_temp.whole(r.payload -> 'nextTimerNumber'), 1), 1000000))::integer
    FROM public.yourlifeone_records r
    WHERE r.collection = 'clock' AND r.record_id = 'meta' AND r.deleted_at IS NULL
    ON CONFLICT DO NOTHING;

    INSERT INTO stopwatch (user_id, id, label, accumulated_ms, started_at, laps, position, created_at, updated_at)
    SELECT r.user_id,
           substr(r.record_id, 11),
           pg_temp.label(r.payload ->> 'label', 'Stopwatch'),
           coalesce(pg_temp.whole(r.payload -> 'accumulatedMs'), 0),
           pg_temp.whole(r.payload -> 'startedAt'),
           coalesce(ARRAY(SELECT pg_temp.whole(lap) FROM jsonb_array_elements(
                              CASE WHEN jsonb_typeof(r.payload -> 'laps') = 'array' THEN r.payload -> 'laps'
                                   ELSE '[]' END) WITH ORDINALITY AS listed(lap, ordinality)
                          WHERE pg_temp.whole(lap) IS NOT NULL ORDER BY ordinality LIMIT 500), '{}'),
           coalesce(r.position, 0)::integer,
           coalesce(r.created_at, now()),
           coalesce(r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'clock' AND r.record_id LIKE 'stopwatch:%' AND r.deleted_at IS NULL
      AND length(r.record_id) BETWEEN 11 AND 266 AND substr(r.record_id, 11) ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;

    INSERT INTO timer (user_id, id, label, duration_ms, remaining_ms, ends_at, status, sound, alerting,
                       completed_at, position, created_at, updated_at)
    SELECT t.user_id, t.id, t.label, t.duration_ms, least(t.remaining_ms, t.duration_ms),
           CASE WHEN t.status = 'running' THEN t.ends_at END,
           t.status, t.sound, t.alerting, t.completed_at, t.position, t.created_at, t.updated_at
    FROM (
        SELECT r.user_id,
               substr(r.record_id, 7) AS id,
               pg_temp.label(r.payload ->> 'label', 'Timer') AS label,
               greatest(1, coalesce(pg_temp.whole(r.payload -> 'durationMs'), 300000)) AS duration_ms,
               coalesce(pg_temp.whole(r.payload -> 'remainingMs'), 0) AS remaining_ms,
               pg_temp.whole(r.payload -> 'endsAt') AS ends_at,
               CASE WHEN r.payload ->> 'status' = 'running' AND pg_temp.whole(r.payload -> 'endsAt') IS NOT NULL
                        THEN 'running'
                    WHEN r.payload ->> 'status' = 'completed' THEN 'completed'
                    ELSE 'idle' END AS status,
               CASE WHEN r.payload ->> 'sound' IN ('chime', 'bell', 'pulse', 'dawn') THEN r.payload ->> 'sound'
                    ELSE 'chime' END AS sound,
               coalesce(r.payload ->> 'alerting' = 'true', false) AS alerting,
               pg_temp.try_instant(r.payload ->> 'completedAt') AS completed_at,
               coalesce(r.position, 0)::integer AS position,
               coalesce(r.created_at, now()) AS created_at,
               coalesce(r.updated_at, now()) AS updated_at
        FROM public.yourlifeone_records r
        WHERE r.collection = 'clock' AND r.record_id LIKE 'timer:%' AND r.deleted_at IS NULL
          AND length(r.record_id) BETWEEN 7 AND 262 AND substr(r.record_id, 7) ~ '^[\x21-\x7E]+$'
    ) t
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.whole(jsonb);
DROP FUNCTION pg_temp.label(text, text);
