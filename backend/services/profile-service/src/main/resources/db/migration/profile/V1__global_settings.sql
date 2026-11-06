-- Account-wide settings that no single domain owns. One row per Supabase user.
CREATE TABLE global_settings (
    user_id    uuid PRIMARY KEY,
    city       text        NOT NULL CHECK (length(city) BETWEEN 1 AND 100),
    country    text        NOT NULL CHECK (length(country) BETWEEN 1 AND 100),
    time_zone  text        CHECK (time_zone IS NULL OR length(time_zone) BETWEEN 1 AND 64),
    updated_at timestamptz NOT NULL
);
