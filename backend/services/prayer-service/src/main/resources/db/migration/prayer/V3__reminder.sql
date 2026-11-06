-- Deadline reminders the service has sent: one per prayer per date, so however often it checks and however
-- many tabs are open, a reminder goes out once. Reminders reach open tabs only (over the live-update stream).
CREATE TABLE reminder (
    user_id     uuid        NOT NULL,
    prayer_date date        NOT NULL,
    prayer_name text        NOT NULL CHECK (prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')),
    deadline_at timestamptz NOT NULL,
    sent_at     timestamptz NOT NULL CHECK (sent_at <= deadline_at),
    PRIMARY KEY (user_id, prayer_date, prayer_name)
);
