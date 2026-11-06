-- App preferences every device of an account shares. Device-only settings stay in the browser.
-- Allowed values (themes, calendar tabs, retention range) are configuration (app.profile.preferences.*);
-- these checks only bound what can be stored.
CREATE TABLE app_preferences (
    user_id              uuid PRIMARY KEY,
    theme                text        NOT NULL CHECK (length(theme) BETWEEN 1 AND 32),
    data_retention_days  integer     NOT NULL CHECK (data_retention_days BETWEEN 1 AND 3650),
    telemetry            boolean     NOT NULL,
    default_calendar_tab text        CHECK (default_calendar_tab IS NULL OR length(default_calendar_tab) BETWEEN 1 AND 32),
    updated_at           timestamptz NOT NULL
);

-- A user's goal tags, in their order. A name appears once per user.
CREATE TABLE goal_tag (
    user_id  uuid    NOT NULL REFERENCES app_preferences (user_id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position BETWEEN 0 AND 999),
    name     text    NOT NULL CHECK (length(name) BETWEEN 1 AND 100 AND name = btrim(name)),
    PRIMARY KEY (user_id, position),
    -- Checked at commit: reordering tags moves names between positions within one save.
    CONSTRAINT goal_tag_name_once UNIQUE (user_id, name) DEFERRABLE INITIALLY DEFERRED
);

-- One connection record per user and provider.
CREATE TABLE integration (
    user_id       uuid        NOT NULL,
    provider      text        NOT NULL CHECK (provider ~ '^[a-z][a-z0-9-]{0,39}$'),
    status        text        NOT NULL CHECK (status IN ('connected', 'disconnected', 'error')),
    configured_at timestamptz,
    last_error    text        CHECK (last_error IS NULL OR length(last_error) BETWEEN 1 AND 500),
    updated_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, provider)
);
