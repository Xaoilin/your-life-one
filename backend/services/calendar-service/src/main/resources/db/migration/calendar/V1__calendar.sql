-- Calendar data is account -> source -> event, scoped to one Supabase user. IDs are text so records
-- the React app created keep their IDs when imported.
CREATE TABLE account (
    user_id        uuid        NOT NULL,
    id             text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256),
    provider       text        NOT NULL CHECK (provider IN ('google', 'local')),
    name           text        NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    email          text        NOT NULL DEFAULT '' CHECK (length(email) <= 320),
    is_primary     boolean     NOT NULL DEFAULT false,
    palette_index  integer,
    -- Google access: connected, needs_reconnect, revoked or error. Local accounts stay connected.
    auth_status    text        NOT NULL DEFAULT 'connected'
                               CHECK (auth_status IN ('connected', 'needs_reconnect', 'revoked', 'error')),
    auth_error     text,
    last_synced_at timestamptz,
    sync_error     text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, id)
);

-- One account per connected Google identity.
CREATE UNIQUE INDEX account_google_email ON account (user_id, email) WHERE provider = 'google';

CREATE TABLE source (
    user_id            uuid        NOT NULL,
    id                 text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256),
    account_id         text        NOT NULL,
    name               text        NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    color              text        NOT NULL CHECK (length(color) BETWEEN 1 AND 32),
    visible            boolean     NOT NULL DEFAULT true,
    -- Set for calendars mirrored from Google; null for local calendars.
    google_calendar_id text,
    access_role        text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, account_id) REFERENCES account (user_id, id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX source_google_calendar ON source (user_id, account_id, google_calendar_id)
    WHERE google_calendar_id IS NOT NULL;

CREATE TABLE event (
    user_id         uuid        NOT NULL,
    id              text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256),
    source_id       text        NOT NULL,
    title           text        NOT NULL CHECK (length(title) BETWEEN 1 AND 500),
    description     text        NOT NULL DEFAULT '',
    location        text,
    all_day         boolean     NOT NULL,
    -- Timed events: the instants. All-day events: UTC midnight of the first day and of the day after
    -- the last day, so an all-day event keeps its calendar dates in every time zone.
    starts_at       timestamptz NOT NULL,
    ends_at         timestamptz NOT NULL CHECK (ends_at >= starts_at),
    google_event_id text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, source_id) REFERENCES source (user_id, id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX event_google ON event (user_id, source_id, google_event_id) WHERE google_event_id IS NOT NULL;
CREATE INDEX event_user_range ON event (user_id, starts_at, ends_at);
