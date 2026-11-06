-- The job search: one row per application (a lead, recruiter contact, application or offer), in the
-- order they were added, and each application's evidence history. Allowed statuses, work types,
-- regions and history kinds are configuration (app.life.jobs.*); these checks bound what is stored.
CREATE TABLE application (
    user_id          uuid        NOT NULL,
    id               text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    position         integer     NOT NULL CHECK (position >= 0),
    company          text        NOT NULL CHECK (length(company) BETWEEN 1 AND 256 AND company = btrim(company)),
    role             text        NOT NULL CHECK (length(role) BETWEEN 1 AND 256 AND role = btrim(role)),
    url              text        CHECK (url IS NULL OR (length(url) <= 2048 AND url ~ '^https?://[^[:space:]/]+[^[:space:]]*$'
                                        AND url !~ '^https?://[^/]*@')),
    work_type        text        NOT NULL CHECK (work_type ~ '^[a-z_]{1,32}$'),
    remote_region    text        NOT NULL CHECK (remote_region ~ '^[a-z_]{1,32}$'),
    remote_status    text        NOT NULL CHECK (remote_status ~ '^[a-z_]{1,32}$'),
    remote_evidence  text        NOT NULL CHECK (length(btrim(remote_evidence)) BETWEEN 1 AND 10000),
    remote_caveat    text        CHECK (remote_caveat IS NULL OR length(remote_caveat) <= 2000),
    compensation     text        CHECK (compensation IS NULL OR length(compensation) <= 2000),
    status           text        NOT NULL CHECK (status ~ '^[a-z_]{1,32}$'),
    application_date date,
    next_action      text        NOT NULL CHECK (length(btrim(next_action)) BETWEEN 1 AND 2000),
    next_action_date date,
    notes            text        NOT NULL DEFAULT '' CHECK (length(notes) <= 10000),
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT application_position_once UNIQUE (user_id, position),
    -- Remote eligibility is only confirmed for a known region.
    CONSTRAINT application_confirmed_remote_region CHECK (remote_status <> 'confirmed' OR remote_region <> 'unknown')
);

CREATE TABLE history_entry (
    user_id        uuid    NOT NULL,
    application_id text    NOT NULL,
    position       integer NOT NULL CHECK (position >= 0),
    id             text    NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    kind           text    NOT NULL CHECK (kind ~ '^[a-z_]{1,32}$'),
    entry_date     date,
    summary        text    NOT NULL CHECK (length(summary) BETWEEN 1 AND 2000 AND summary = btrim(summary)),
    details        text    NOT NULL DEFAULT '' CHECK (length(details) <= 10000),
    evidence_url   text    CHECK (evidence_url IS NULL OR (length(evidence_url) <= 2048
                                  AND evidence_url ~ '^https?://[^[:space:]/]+[^[:space:]]*$')),
    PRIMARY KEY (user_id, application_id, position),
    FOREIGN KEY (user_id, application_id) REFERENCES application (user_id, id) ON DELETE CASCADE,
    CONSTRAINT history_entry_id_once UNIQUE (user_id, application_id, id)
);

-- The same evidence link is recorded once per application.
CREATE UNIQUE INDEX history_entry_evidence_once ON history_entry (user_id, application_id, evidence_url)
    WHERE evidence_url IS NOT NULL;
