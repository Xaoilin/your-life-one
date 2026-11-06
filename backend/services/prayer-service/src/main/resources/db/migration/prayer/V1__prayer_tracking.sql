-- Prayer preferences and tracking state. One row per Supabase user.
-- Location (city, country, time zone) is a global setting owned by the profile service.
CREATE TABLE profile (
    user_id             uuid PRIMARY KEY,
    enabled             boolean     NOT NULL,
    reminder_enabled    boolean     NOT NULL,
    -- Allowed values are configuration (app.prayer.reminder-minutes-options), not schema.
    reminder_minutes    integer     NOT NULL CHECK (reminder_minutes > 0),
    tracking_started_at timestamptz NOT NULL,
    -- Activation-day eligibility: prayers still open when tracking started on activation_date.
    activation_date     date,
    activation_prayers  text,
    imported_at         timestamptz,
    updated_at          timestamptz NOT NULL
);

-- Shared cache of provider timetables: one row per location, method and date. Not user data.
CREATE TABLE schedule_day (
    id            uuid PRIMARY KEY,
    city          text        NOT NULL,
    country       text        NOT NULL,
    method        integer     NOT NULL,
    schedule_date date        NOT NULL,
    time_zone     text        NOT NULL,
    hijri_date    text        NOT NULL,
    fajr          time        NOT NULL,
    sunrise       time        NOT NULL,
    dhuhr         time        NOT NULL,
    asr           time        NOT NULL,
    sunset        time        NOT NULL,
    maghrib       time        NOT NULL,
    isha          time        NOT NULL,
    midnight      time        NOT NULL,
    fetched_at    timestamptz NOT NULL
);
CREATE UNIQUE INDEX schedule_day_location_date ON schedule_day (lower(city), lower(country), method, schedule_date);

-- Which timetable applied to a user on a date, so history keeps the location they were in.
CREATE TABLE user_day (
    user_id         uuid NOT NULL,
    day             date NOT NULL,
    schedule_day_id uuid NOT NULL REFERENCES schedule_day (id),
    PRIMARY KEY (user_id, day)
);

CREATE TABLE outcome (
    id          uuid PRIMARY KEY,
    user_id     uuid        NOT NULL,
    prayer_date date        NOT NULL,
    prayer_name text        NOT NULL CHECK (prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')),
    status      text        NOT NULL CHECK (status IN ('on_time', 'late', 'missed', 'unclassified')),
    recorded_at timestamptz NOT NULL,
    source      text,
    task_id     text,
    -- One-time reward receipt: set when first completed, never cleared by corrections.
    rewarded    boolean     NOT NULL DEFAULT false,
    -- On-time deadline in force when recorded; null when the timetable was unknown.
    deadline_at timestamptz,
    CONSTRAINT outcome_one_per_prayer UNIQUE (user_id, prayer_date, prayer_name)
);
