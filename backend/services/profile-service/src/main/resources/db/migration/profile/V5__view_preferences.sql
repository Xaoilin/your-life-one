-- Per-account presentation choices. No dates, drafts, searches, record IDs or secret values.
-- The user/key PK also covers ordered account reads and atomic point updates.
CREATE TABLE view_preferences (
    user_id          uuid        NOT NULL,
    preference_key   text        NOT NULL CHECK (length(preference_key) BETWEEN 1 AND 80),
    preference_value text        NOT NULL CHECK (length(preference_value) BETWEEN 1 AND 64),
    updated_at       timestamptz NOT NULL,
    PRIMARY KEY (user_id, preference_key)
);

-- This is a private service-owned schema, never a browser Data API table.
REVOKE ALL ON view_preferences FROM PUBLIC;
ALTER TABLE view_preferences ENABLE ROW LEVEL SECURITY;
