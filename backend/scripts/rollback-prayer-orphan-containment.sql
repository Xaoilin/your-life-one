-- Controller-owned rollback only, with the scheduler paused; see docs/KAN-324.md.
-- Run in one transaction with search_path set to prayer. Retain the containment receipt.
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
