-- One-time import of the progress the React app kept in Supabase's public.yourlifeone_records (collection
-- gamification): the profile summary, habit tallies, the daily log, the prayer reward ledger and the
-- daily momentum pillars kept inside the profile record. A no-op where that table does not exist.
-- Temporary: once the React app reads progress only from this service, yourlifeone_records rows for it are no
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

CREATE OR REPLACE FUNCTION pg_temp.whole(value jsonb) RETURNS bigint LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN jsonb_typeof(value) = 'number' AND (value #>> '{}') ~ '^[0-9]{1,15}(\.0+)?$'
                THEN floor((value #>> '{}')::numeric)::bigint ELSE 0 END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO profile (user_id, total_xp, level, current_streak, longest_streak, last_completion_date,
                         total_tasks_completed, badges, updated_at)
    SELECT r.user_id,
           pg_temp.whole(r.payload -> 'totalXp'),
           greatest(1, floor(sqrt(pg_temp.whole(r.payload -> 'totalXp') / 25.0))::integer),
           least(pg_temp.whole(r.payload -> 'currentStreak'), 1000000)::integer,
           greatest(pg_temp.whole(r.payload -> 'longestStreak'), pg_temp.whole(r.payload -> 'currentStreak'))::integer,
           pg_temp.try_date(r.payload ->> 'lastCompletionDate'),
           pg_temp.whole(r.payload -> 'totalTasksCompleted')::integer,
           coalesce(ARRAY(SELECT DISTINCT badge FROM jsonb_array_elements_text(
                              CASE WHEN jsonb_typeof(r.payload -> 'badges') = 'array' THEN r.payload -> 'badges'
                                   ELSE '[]' END) AS listed(badge)
                          WHERE length(badge) BETWEEN 1 AND 64 LIMIT 500), '{}'),
           coalesce(r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'gamification' AND r.record_id = 'profile' AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
    ON CONFLICT DO NOTHING;

    INSERT INTO habit_tally (user_id, task_id, count)
    SELECT r.user_id, substr(r.record_id, 7), pg_temp.whole(r.payload -> 'count')::integer
    FROM public.yourlifeone_records r
    WHERE r.collection = 'gamification' AND r.record_id LIKE 'habit:%' AND r.deleted_at IS NULL
      AND length(r.record_id) BETWEEN 7 AND 262 AND pg_temp.whole(r.payload -> 'count') > 0
    ON CONFLICT DO NOTHING;

    INSERT INTO daily_log (user_id, day, task_id)
    SELECT DISTINCT r.user_id, pg_temp.try_date(substr(r.record_id, 5)), task.id
    FROM public.yourlifeone_records r,
         jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'taskIds') = 'array'
                                        THEN r.payload -> 'taskIds' ELSE '[]' END) AS task(id)
    WHERE r.collection = 'gamification' AND r.record_id LIKE 'day:%' AND r.deleted_at IS NULL
      AND pg_temp.try_date(substr(r.record_id, 5)) IS NOT NULL
      AND length(task.id) BETWEEN 1 AND 256
    ON CONFLICT DO NOTHING;

    -- Every ledger entry becomes a receipt, rewarded or not, so no prayer the app already handled is
    -- rewarded again. Its level and streak at the time were not recorded.
    INSERT INTO prayer_reward (user_id, prayer_date, prayer_name, source, task_id, log_id, xp_earned, level,
                               current_streak, recorded_at, granted_at)
    SELECT r.user_id,
           pg_temp.try_date(r.payload ->> 'date'),
           r.payload ->> 'prayerName',
           'import',
           nullif(left(r.payload ->> 'taskId', 256), ''),
           coalesce(nullif(left(r.payload ->> 'taskId', 256), ''), 'prayer:' || lower(r.payload ->> 'prayerName')),
           least(pg_temp.whole(r.payload -> 'xpEarned'), 100000)::integer,
           1,
           0,
           coalesce(pg_temp.try_instant(r.payload ->> 'recordedAt'), r.created_at, now()),
           coalesce(r.created_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'gamification' AND r.record_id LIKE 'prayer:%' AND r.deleted_at IS NULL
      AND pg_temp.try_date(r.payload ->> 'date') IS NOT NULL
      AND r.payload ->> 'prayerName' IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')
    ON CONFLICT DO NOTHING;

    INSERT INTO momentum (user_id, pillar, state, version, updated_at)
    SELECT r.user_id, pillar.name, r.payload -> pillar.field, 1, coalesce(r.updated_at, now())
    FROM public.yourlifeone_records r,
         (VALUES ('learn', 'dailyMomentumLearn'), ('move', 'dailyMomentumMove')) AS pillar(name, field)
    WHERE r.collection = 'gamification' AND r.record_id = 'profile' AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload -> pillar.field) = 'object'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.whole(jsonb);
