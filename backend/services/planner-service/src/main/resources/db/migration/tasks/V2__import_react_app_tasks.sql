-- One-time import of the tasks the React app kept in Supabase's public.yourlifeone_records (collection tasks).
-- A no-op where that table does not exist (tests, fresh databases). Tasks are normalised the way the app
-- showed them: a daily task titled after a prayer is that prayer's habit, a task that is not completed
-- keeps no completion, and a completed one without its local date takes its UTC date.
-- Temporary: once the React app reads tasks only from this service, yourlifeone_records rows for them are no
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

CREATE OR REPLACE FUNCTION pg_temp.prayer_of(category text, prayer text, title text) RETURNS text
    LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE
        WHEN prayer IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha') THEN prayer
        WHEN category NOT IN ('prayer', 'daily') THEN NULL
        WHEN title ~* '\mfajr\M' THEN 'Fajr'
        WHEN title ~* '\mdhuhr\M' THEN 'Dhuhr'
        WHEN title ~* '\masr\M' THEN 'Asr'
        WHEN title ~* '\mmaghrib\M' THEN 'Maghrib'
        WHEN title ~* '\misha\M' THEN 'Isha'
    END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO task (user_id, id, title, description, category, priority, completed, completed_at,
                      completed_local_date, completion_time_zone, due_date, recurrence, last_reset, prayer_name,
                      goal_tag, emoji, project_id, workflow_state, blocked_reason, board_order, created_at, updated_at)
    SELECT t.user_id, t.id,
           CASE WHEN t.prayer IS NOT NULL THEN t.prayer || ' Prayer' ELSE t.title END,
           t.description, t.category, t.priority, t.completed,
           CASE WHEN t.completed THEN t.completed_at END,
           CASE WHEN t.completed THEN coalesce(t.local_date, (t.completed_at AT TIME ZONE 'UTC')::date) END,
           CASE WHEN t.completed THEN coalesce(t.zone, 'UTC') END,
           CASE WHEN t.category IN ('task', 'goal') THEN t.due_date END,
           t.recurrence,
           CASE WHEN t.recurrence IS NOT NULL THEN t.last_reset END,
           t.prayer, t.goal_tag, t.emoji,
           CASE WHEN t.category IN ('task', 'goal') THEN t.project_id END,
           CASE WHEN t.category = 'task' AND t.project_id IS NOT NULL THEN t.workflow_state END,
           CASE WHEN t.category = 'task' AND t.project_id IS NOT NULL THEN t.blocked_reason END,
           CASE WHEN t.category = 'task' AND t.project_id IS NOT NULL THEN t.board_order END,
           t.created_at, t.updated_at
    FROM (
        SELECT r.user_id,
               r.record_id AS id,
               coalesce(nullif(left(btrim(r.payload ->> 'title'), 300), ''), 'Untitled task') AS title,
               left(coalesce(r.payload ->> 'description', ''), 4000) AS description,
               CASE WHEN pg_temp.prayer_of(r.payload ->> 'category', r.payload ->> 'prayerName', r.payload ->> 'title')
                         IS NOT NULL THEN 'prayer'
                    WHEN r.payload ->> 'category' IN ('daily', 'task', 'goal') THEN r.payload ->> 'category'
                    ELSE 'task' END AS category,
               pg_temp.prayer_of(r.payload ->> 'category', r.payload ->> 'prayerName', r.payload ->> 'title') AS prayer,
               CASE WHEN r.payload ->> 'priority' IN ('low', 'medium', 'high') THEN r.payload ->> 'priority'
                    ELSE 'medium' END AS priority,
               coalesce(r.payload ->> 'completed' = 'true', false)
                   AND pg_temp.try_instant(r.payload ->> 'completedAt') IS NOT NULL AS completed,
               pg_temp.try_instant(r.payload ->> 'completedAt') AS completed_at,
               pg_temp.try_date(r.payload ->> 'completedLocalDate') AS local_date,
               nullif(left(r.payload ->> 'completionTimeZone', 64), '') AS zone,
               pg_temp.try_date(r.payload ->> 'dueDate') AS due_date,
               CASE WHEN pg_temp.prayer_of(r.payload ->> 'category', r.payload ->> 'prayerName', r.payload ->> 'title')
                         IS NOT NULL THEN 'daily'
                    WHEN r.payload ->> 'category' = 'daily'
                        THEN coalesce(CASE WHEN r.payload -> 'recurring' ->> 'frequency' IN ('daily', 'weekdays', 'weekly')
                                           THEN r.payload -> 'recurring' ->> 'frequency' END, 'daily')
                    WHEN r.payload -> 'recurring' ->> 'frequency' IN ('daily', 'weekdays', 'weekly')
                        THEN r.payload -> 'recurring' ->> 'frequency' END AS recurrence,
               pg_temp.try_date(r.payload -> 'recurring' ->> 'lastReset') AS last_reset,
               nullif(left(btrim(r.payload ->> 'goalTag'), 64), '') AS goal_tag,
               nullif(left(r.payload ->> 'emoji', 16), '') AS emoji,
               nullif(left(r.payload ->> 'projectId', 256), '') AS project_id,
               CASE WHEN r.payload ->> 'workflowState' IN ('backlog', 'next_up', 'in_progress', 'blocked')
                    THEN r.payload ->> 'workflowState' END AS workflow_state,
               nullif(left(btrim(r.payload ->> 'blockedReason'), 1000), '') AS blocked_reason,
               CASE WHEN jsonb_typeof(r.payload -> 'boardOrder') = 'number' AND (r.payload ->> 'boardOrder') ~ '^[0-9]{1,9}$'
                    THEN (r.payload ->> 'boardOrder')::integer END AS board_order,
               coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()) AS created_at,
               coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now()) AS updated_at
        FROM public.yourlifeone_records r
        WHERE r.collection = 'tasks'
          AND r.deleted_at IS NULL
          AND jsonb_typeof(r.payload) = 'object'
          AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ) t
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.prayer_of(text, text, text);
