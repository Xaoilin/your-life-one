-- The clock workspace: stopwatches with laps and countdown timers, in the order they were added. Times
-- are epoch milliseconds, as the app's clock keeps them. Allowed sounds and statuses are configuration
-- (app.planner.clock.*); these checks bound what is stored.
CREATE TABLE stopwatch (
    user_id        uuid        NOT NULL,
    id             text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    label          text        NOT NULL CHECK (length(label) BETWEEN 1 AND 100),
    accumulated_ms bigint      NOT NULL CHECK (accumulated_ms >= 0),
    started_at     bigint      CHECK (started_at >= 0),
    laps           bigint[]    NOT NULL DEFAULT '{}' CHECK (cardinality(laps) <= 500),
    position       integer     NOT NULL CHECK (position >= 0),
    created_at     timestamptz NOT NULL,
    updated_at     timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE TABLE timer (
    user_id      uuid        NOT NULL,
    id           text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    label        text        NOT NULL CHECK (length(label) BETWEEN 1 AND 100),
    duration_ms  bigint      NOT NULL CHECK (duration_ms > 0),
    remaining_ms bigint      NOT NULL CHECK (remaining_ms >= 0 AND remaining_ms <= duration_ms),
    ends_at      bigint      CHECK (ends_at >= 0),
    status       text        NOT NULL CHECK (status ~ '^[a-z][a-z_-]{0,31}$'),
    sound        text        NOT NULL CHECK (sound ~ '^[a-z][a-z_-]{0,31}$'),
    alerting     boolean     NOT NULL DEFAULT false,
    completed_at timestamptz,
    position     integer     NOT NULL CHECK (position >= 0),
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    -- Only a running timer has an end.
    CONSTRAINT timer_end_only_while_running CHECK ((status = 'running') = (ends_at IS NOT NULL))
);

-- The numbers the next new stopwatch and timer are named after ("Stopwatch 3").
CREATE TABLE counter (
    user_id               uuid PRIMARY KEY,
    next_stopwatch_number integer NOT NULL DEFAULT 1 CHECK (next_stopwatch_number >= 1),
    next_timer_number     integer NOT NULL DEFAULT 1 CHECK (next_timer_number >= 1)
);
