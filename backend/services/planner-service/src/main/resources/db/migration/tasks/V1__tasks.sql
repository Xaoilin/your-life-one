-- Tasks, daily habits, prayer habits and goals, with project-board state. Allowed categories,
-- priorities and states are configuration (app.planner.tasks.*); these checks bound what is stored.
CREATE TABLE task (
    user_id              uuid        NOT NULL,
    id                   text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    title                text        NOT NULL CHECK (length(title) BETWEEN 1 AND 300 AND title = btrim(title)),
    description          text        NOT NULL DEFAULT '' CHECK (length(description) <= 4000),
    category             text        NOT NULL CHECK (category ~ '^[a-z][a-z_-]{0,31}$'),
    priority             text        NOT NULL CHECK (priority ~ '^[a-z][a-z_-]{0,31}$'),
    completed            boolean     NOT NULL DEFAULT false,
    completed_at         timestamptz,
    completed_local_date date,
    completion_time_zone text        CHECK (length(completion_time_zone) <= 64),
    due_date             date,
    -- A habit (daily or prayer) repeats; last_reset is the day it was last reopened or completed.
    recurrence           text        CHECK (recurrence ~ '^[a-z][a-z_-]{0,31}$'),
    last_reset           date,
    prayer_name          text        CHECK (prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')),
    goal_tag             text        CHECK (length(goal_tag) BETWEEN 1 AND 64),
    emoji                text        CHECK (length(emoji) BETWEEN 1 AND 16),
    project_id           text        CHECK (length(project_id) BETWEEN 1 AND 256),
    workflow_state       text        CHECK (workflow_state ~ '^[a-z][a-z_-]{0,31}$'),
    blocked_reason       text        CHECK (length(blocked_reason) BETWEEN 1 AND 1000),
    board_order          integer     CHECK (board_order >= 0),
    created_at           timestamptz NOT NULL,
    updated_at           timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    -- Only a completed task carries a completion.
    CONSTRAINT task_completion_only_when_completed
        CHECK (completed OR (completed_at IS NULL AND completed_local_date IS NULL AND completion_time_zone IS NULL)),
    CONSTRAINT task_completion_is_stamped
        CHECK (NOT completed OR (completed_at IS NOT NULL AND completed_local_date IS NOT NULL
                                 AND completion_time_zone IS NOT NULL)),
    -- Prayer habits name their prayer; nothing else does.
    CONSTRAINT task_prayer_names_its_prayer CHECK ((category = 'prayer') = (prayer_name IS NOT NULL)),
    CONSTRAINT task_last_reset_needs_recurrence CHECK (recurrence IS NOT NULL OR last_reset IS NULL)
);

-- One prayer habit per prayer, so a prayer is never completed or rewarded twice through two tasks.
CREATE UNIQUE INDEX task_one_habit_per_prayer ON task (user_id, prayer_name) WHERE category = 'prayer';
CREATE INDEX task_by_project ON task (user_id, project_id) WHERE project_id IS NOT NULL;

-- The time zones the daily reset uses for each user: the app's and the prayer timetable's. The app
-- reports them when it loads; the service then reopens habits on each new day by itself.
CREATE TABLE day_zone (
    user_id          uuid PRIMARY KEY,
    app_time_zone    text        NOT NULL CHECK (length(app_time_zone) BETWEEN 1 AND 64),
    prayer_time_zone text        CHECK (length(prayer_time_zone) BETWEEN 1 AND 64),
    updated_at       timestamptz NOT NULL
);
