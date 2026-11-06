-- Server-decided reminders beyond the deadline one: a snooze on every reminder, the bounded notices
-- (a prayer's opportunity and the Learn/Move momentum prompts) and the momentum reminder preferences.

-- A reminder may be snoozed once; when the snooze ends and it is still active it is sent again, once.
ALTER TABLE reminder
    ADD COLUMN snoozed_until timestamptz,
    ADD COLUMN snooze_count  integer NOT NULL DEFAULT 0 CHECK (snooze_count BETWEEN 0 AND 1),
    ADD COLUMN resent_at     timestamptz,
    ADD CONSTRAINT reminder_snooze_recorded CHECK ((snooze_count = 1) = (snoozed_until IS NOT NULL)),
    ADD CONSTRAINT reminder_resent_after_snooze CHECK (resent_at IS NULL OR snoozed_until IS NOT NULL);

-- Receipts of sent bounded notices: one row per prayer opportunity, and one per pillar of a momentum
-- notice (pillars prompted at the same instant share one notice). A receipt is claimed once, so a notice
-- goes out once however often the service checks.
CREATE TABLE notice (
    user_id       uuid        NOT NULL,
    key           text        NOT NULL CHECK (length(key) BETWEEN 1 AND 200 AND key ~ '^[\x21-\x7E]+$'),
    kind          text        NOT NULL CHECK (kind IN ('prayer-opportunity', 'momentum')),
    prayer_date   date        NOT NULL,
    -- The prayer whose start the notice follows.
    prayer_name   text        NOT NULL CHECK (prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')),
    pillar        text        CHECK (pillar IN ('learn', 'move')),
    fires_at      timestamptz NOT NULL,
    expires_at    timestamptz NOT NULL,
    sent_at       timestamptz NOT NULL,
    snoozed_until timestamptz,
    snooze_count  integer     NOT NULL DEFAULT 0 CHECK (snooze_count BETWEEN 0 AND 1),
    resent_at     timestamptz,
    PRIMARY KEY (user_id, key),
    CONSTRAINT notice_expires_after_firing CHECK (expires_at > fires_at),
    CONSTRAINT notice_pillar_only_for_momentum CHECK ((kind = 'momentum') = (pillar IS NOT NULL)),
    CONSTRAINT notice_snooze_recorded CHECK ((snooze_count = 1) = (snoozed_until IS NOT NULL)),
    CONSTRAINT notice_resent_after_snooze CHECK (resent_at IS NULL OR snoozed_until IS NOT NULL)
);

-- The same moment is receipted once, whichever key (imported or current) it was sent under.
CREATE UNIQUE INDEX notice_one_per_moment
    ON notice (user_id, kind, prayer_date, prayer_name, coalesce(pillar, ''), fires_at);
CREATE INDEX notice_by_expiry ON notice (user_id, expires_at);

-- The app's momentum reminder preferences per pillar; a pillar without a row uses the configured defaults
-- (app.prayer.reminders.default-anchors).
CREATE TABLE momentum_reminder (
    user_id       uuid        NOT NULL,
    pillar        text        NOT NULL CHECK (pillar IN ('learn', 'move')),
    enabled       boolean     NOT NULL,
    after_prayers text[]      NOT NULL CHECK (
        cardinality(after_prayers) <= 5
        AND after_prayers <@ ARRAY['Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha']::text[]),
    -- The latest date whose Level 1 is complete; no momentum notice is sent for it.
    completed_on  date,
    updated_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, pillar)
);
