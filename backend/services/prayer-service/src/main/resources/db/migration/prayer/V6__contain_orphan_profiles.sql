-- One-time reversible containment (KAN-324). Keep the receipt until account-data retention is governed.
-- Supabase supplies auth.users before this service migrates. An unavailable/filtered identity source
-- must abort the transaction, never turn an unknown identity into a deleted one.
DO $$
BEGIN
    IF NOT has_table_privilege(current_user, 'auth.users', 'SELECT')
        OR row_security_active('auth.users'::regclass) THEN
        RAISE EXCEPTION 'Orphan containment requires unfiltered Auth identity visibility';
    END IF;
END $$;

CREATE TABLE orphan_profile_containment (
    user_id             uuid PRIMARY KEY,
    previous_enabled    boolean NOT NULL,
    previous_updated_at timestamptz NOT NULL,
    contained_at        timestamptz NOT NULL,
    restored_at         timestamptz
);
ALTER TABLE orphan_profile_containment ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON orphan_profile_containment FROM PUBLIC;

-- Serialize the before-image with preference writes. No historical rows or Profile settings are deleted.
LOCK TABLE profile IN SHARE ROW EXCLUSIVE MODE;
WITH contained AS (
    INSERT INTO orphan_profile_containment (user_id, previous_enabled, previous_updated_at, contained_at)
    SELECT p.user_id, p.enabled, p.updated_at, transaction_timestamp()
    FROM profile p
    WHERE p.enabled AND NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = p.user_id)
    RETURNING user_id, contained_at
)
UPDATE profile p SET enabled = false, updated_at = c.contained_at
FROM contained c WHERE p.user_id = c.user_id;
