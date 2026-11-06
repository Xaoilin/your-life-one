-- XP, level, streaks and badges: one profile row per user, locked while a reward is applied, so two
-- rewards at once never lose XP.
CREATE TABLE profile (
    user_id               uuid PRIMARY KEY,
    total_xp              bigint      NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    level                 integer     NOT NULL DEFAULT 1 CHECK (level >= 1),
    current_streak        integer     NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak        integer     NOT NULL DEFAULT 0 CHECK (longest_streak >= current_streak),
    last_completion_date  date,
    total_tasks_completed integer     NOT NULL DEFAULT 0 CHECK (total_tasks_completed >= 0),
    badges                text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(badges) <= 500),
    updated_at            timestamptz NOT NULL
);

-- How many times each habit has been completed.
CREATE TABLE habit_tally (
    user_id uuid    NOT NULL,
    task_id text    NOT NULL CHECK (length(task_id) BETWEEN 1 AND 256),
    count   integer NOT NULL CHECK (count > 0),
    PRIMARY KEY (user_id, task_id)
);

-- Which habits were rewarded on which day: a habit earns XP at most once a day.
CREATE TABLE daily_log (
    user_id uuid NOT NULL,
    day     date NOT NULL,
    task_id text NOT NULL CHECK (length(task_id) BETWEEN 1 AND 256),
    PRIMARY KEY (user_id, day, task_id)
);

-- The reward receipt for each prayer the prayer service recorded as prayed: its primary key is what
-- makes a prayer earn XP exactly once. Receipts granted here (source 'planner') are taken back if the
-- prayer's outcome is undone; imported ones, and any from before a progress reset, are history and are kept.
CREATE TABLE prayer_reward (
    user_id          uuid        NOT NULL,
    prayer_date      date        NOT NULL,
    prayer_name      text        NOT NULL CHECK (prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')),
    source           text        NOT NULL CHECK (source IN ('planner', 'import', 'reset')),
    task_id          text        CHECK (length(task_id) BETWEEN 1 AND 256),
    -- The id the daily log and habit tally used for this reward.
    log_id           text        NOT NULL CHECK (length(log_id) BETWEEN 1 AND 256),
    xp_earned        integer     NOT NULL DEFAULT 0 CHECK (xp_earned >= 0),
    new_badges       text[]      NOT NULL DEFAULT '{}',
    level            integer     NOT NULL CHECK (level >= 1),
    leveled_up       boolean     NOT NULL DEFAULT false,
    current_streak   integer     NOT NULL CHECK (current_streak >= 0),
    streak_milestone boolean     NOT NULL DEFAULT false,
    -- Whether granting it completed that day's prayer habit, which is reopened if the reward is taken back.
    completed_task   boolean     NOT NULL DEFAULT false,
    recorded_at      timestamptz NOT NULL,
    granted_at       timestamptz NOT NULL,
    PRIMARY KEY (user_id, prayer_date, prayer_name)
);

-- Daily momentum (learn and move pillars): the app's own state for each pillar, replaced whole. The
-- version makes a save from an out-of-date copy fail instead of overwriting newer progress.
CREATE TABLE momentum (
    user_id    uuid        NOT NULL,
    pillar     text        NOT NULL CHECK (pillar ~ '^[a-z]{1,32}$'),
    state      jsonb       NOT NULL CHECK (jsonb_typeof(state) = 'object'),
    version    integer     NOT NULL CHECK (version >= 0),
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, pillar)
);
