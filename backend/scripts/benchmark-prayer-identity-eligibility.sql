-- Synthetic local PostgreSQL only. No production records; all effects roll back.
BEGIN;
SET LOCAL statement_timeout = '5s';
CREATE SCHEMA auth;
CREATE TABLE auth.users (id uuid PRIMARY KEY);
CREATE SCHEMA prayer;
SET LOCAL search_path = prayer;
\ir ../services/prayer-service/src/main/resources/db/migration/prayer/V1__prayer_tracking.sql
INSERT INTO profile (user_id, enabled, reminder_enabled, reminder_minutes, tracking_started_at, updated_at)
SELECT md5(n::text)::uuid, true, true, 15, now(), now() FROM generate_series(1, 36) n;
INSERT INTO auth.users VALUES (md5('1')::uuid), (md5('37')::uuid);
ANALYZE profile;
ANALYZE auth.users;
-- Old scheduler: 36 candidate rows; revised scheduler: 1, with no new index.
EXPLAIN (ANALYZE, BUFFERS) SELECT user_id FROM prayer.profile WHERE enabled;
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.user_id FROM prayer.profile p
WHERE p.enabled AND EXISTS (SELECT 1 FROM auth.users u WHERE u.id = p.user_id);
EXPLAIN (ANALYZE, BUFFERS)
SELECT id FROM auth.users WHERE id = md5('1')::uuid FOR KEY SHARE;
-- Exact migration statement, including its before-image insert and selective update.
CREATE TABLE orphan_profile_containment (
    user_id uuid PRIMARY KEY, previous_enabled boolean NOT NULL,
    previous_updated_at timestamptz NOT NULL, contained_at timestamptz NOT NULL, restored_at timestamptz
);
EXPLAIN (ANALYZE, BUFFERS)
WITH contained AS (
    INSERT INTO orphan_profile_containment (user_id, previous_enabled, previous_updated_at, contained_at)
    SELECT p.user_id, p.enabled, p.updated_at, transaction_timestamp()
    FROM profile p
    WHERE p.enabled AND NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = p.user_id)
    RETURNING user_id, contained_at
)
UPDATE profile p SET enabled = false, updated_at = c.contained_at
FROM contained c WHERE p.user_id = c.user_id;
EXPLAIN (ANALYZE, BUFFERS)
WITH restored AS (
    UPDATE profile p
    SET enabled = c.previous_enabled, updated_at = c.previous_updated_at
    FROM orphan_profile_containment c
    WHERE p.user_id = c.user_id AND NOT p.enabled AND p.updated_at = c.contained_at
        AND c.restored_at IS NULL
    RETURNING p.user_id
)
UPDATE orphan_profile_containment c SET restored_at = transaction_timestamp()
FROM restored r WHERE c.user_id = r.user_id;
ROLLBACK;
