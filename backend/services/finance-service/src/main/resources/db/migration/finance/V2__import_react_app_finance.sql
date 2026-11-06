-- One-time import of the ledger and the banking review the React app kept in Supabase's
-- public.yourlifeone_records (collections financeAccounts, transactions, financeBudgets, savingsGoals and
-- financeReviews; deleted rows are skipped). A no-op where that table does not exist (tests, fresh
-- databases). Balances are imported as the app showed them (they already include every transaction), so
-- nothing is applied again. A transaction whose account is gone cannot be kept; a transfer whose other
-- account is gone is kept as an expense.
-- Temporary: once the React app reads finance only from this service, yourlifeone_records rows for it are no
-- longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_date(value text) RETURNS date LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN value ~ '^\d{4}-\d{2}-\d{2}$' THEN value::date END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

-- Whole pence from a JSON number, rounded; null for anything else or anything out of range.
CREATE OR REPLACE FUNCTION pg_temp.pence(value jsonb) RETURNS bigint LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN jsonb_typeof(value) = 'number' AND abs((value #>> '{}')::numeric) <= 1000000000000000
                THEN round((value #>> '{}')::numeric)::bigint END
$$;

CREATE OR REPLACE FUNCTION pg_temp.valid_id(value text) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
    SELECT value IS NOT NULL AND length(value) BETWEEN 1 AND 256 AND value ~ '^[\x21-\x7E]+$'
$$;

CREATE OR REPLACE FUNCTION pg_temp.slug(value text, fallback text) RETURNS text LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN value ~ '^[a-z][a-z-]{0,63}$' THEN value ELSE fallback END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO account (user_id, id, name, type, balance, currency, color, icon, include_in_net_worth, sort_order,
                         created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 200), ''), 'Account'),
           CASE WHEN r.payload ->> 'type' ~ '^[a-z][a-z-]{0,31}$' THEN r.payload ->> 'type' ELSE 'current' END,
           coalesce(pg_temp.pence(r.payload -> 'balance'), 0),
           CASE WHEN r.payload ->> 'currency' ~ '^[A-Z]{3}$' THEN r.payload ->> 'currency' ELSE 'GBP' END,
           left(coalesce(r.payload ->> 'color', ''), 64),
           left(coalesce(r.payload ->> 'icon', ''), 64),
           coalesce(r.payload ->> 'includeInNetWorth', 'true') <> 'false',
           greatest(0, least(coalesce(pg_temp.pence(r.payload -> 'sortOrder'), r.position, 0), 2147483647))::integer,
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'financeAccounts' AND r.deleted_at IS NULL AND jsonb_typeof(r.payload) = 'object'
      AND pg_temp.valid_id(r.record_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO transaction (user_id, id, type, amount, category, account_id, to_account_id, description, date, tags,
                             created_at, updated_at)
    SELECT t.user_id, t.id,
           CASE WHEN t.requested_type = 'transfer' AND t.to_account_id IS NOT NULL THEN 'transfer'
                WHEN t.requested_type = 'income' THEN 'income'
                ELSE 'expense' END,
           t.amount,
           pg_temp.slug(t.category, CASE WHEN t.requested_type = 'income' THEN 'other-income'
                                         WHEN t.requested_type = 'transfer' AND t.to_account_id IS NOT NULL
                                             THEN 'transfer'
                                         ELSE 'other-expense' END),
           t.account_id,
           CASE WHEN t.requested_type = 'transfer' THEN t.to_account_id END,
           t.description, t.date, t.tags, t.created_at, t.updated_at
    FROM (
        SELECT r.user_id,
               r.record_id AS id,
               r.payload ->> 'type' AS requested_type,
               abs(pg_temp.pence(r.payload -> 'amount')) AS amount,
               r.payload ->> 'category' AS category,
               r.payload ->> 'accountId' AS account_id,
               (SELECT a.id FROM account a
                WHERE a.user_id = r.user_id AND a.id = r.payload ->> 'toAccountId'
                  AND a.id IS DISTINCT FROM r.payload ->> 'accountId') AS to_account_id,
               left(coalesce(r.payload ->> 'description', ''), 4000) AS description,
               coalesce(pg_temp.try_date(r.payload ->> 'date'),
                        (coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now())
                            AT TIME ZONE 'UTC')::date) AS date,
               coalesce(ARRAY(SELECT left(btrim(tag), 64)
                              FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'tags') = 'array'
                                                                  THEN r.payload -> 'tags' ELSE '[]' END)
                                   WITH ORDINALITY AS listed(tag, ordinality)
                              WHERE btrim(tag) <> '' ORDER BY ordinality LIMIT 20), '{}') AS tags,
               coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()) AS created_at,
               coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now()) AS updated_at
        FROM public.yourlifeone_records r
        WHERE r.collection = 'transactions' AND r.deleted_at IS NULL AND jsonb_typeof(r.payload) = 'object'
          AND pg_temp.valid_id(r.record_id)
    ) t
    WHERE t.amount BETWEEN 0 AND 1000000000000
      AND EXISTS (SELECT 1 FROM account a WHERE a.user_id = t.user_id AND a.id = t.account_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO budget (user_id, id, category, monthly_limit, created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           pg_temp.slug(r.payload ->> 'category', 'other-expense'),
           greatest(0, least(coalesce(pg_temp.pence(r.payload -> 'monthlyLimit'), 0), 1000000000000)),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'financeBudgets' AND r.deleted_at IS NULL AND jsonb_typeof(r.payload) = 'object'
      AND pg_temp.valid_id(r.record_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO savings_goal (user_id, id, name, target_amount, current_amount, linked_account_id, icon, deadline,
                              completed, completed_at, created_at, updated_at)
    SELECT g.user_id, g.id, g.name, g.target_amount, g.current_amount, g.linked_account_id, g.icon, g.deadline,
           g.completed, CASE WHEN g.completed THEN coalesce(g.completed_at, g.updated_at) END,
           g.created_at, g.updated_at
    FROM (
        SELECT r.user_id,
               r.record_id AS id,
               coalesce(nullif(left(btrim(r.payload ->> 'name'), 200), ''), 'Savings goal') AS name,
               greatest(0, least(coalesce(pg_temp.pence(r.payload -> 'targetAmount'), 0), 1000000000000)) AS target_amount,
               greatest(0, least(coalesce(pg_temp.pence(r.payload -> 'currentAmount'), 0), 1000000000000)) AS current_amount,
               (SELECT a.id FROM account a
                WHERE a.user_id = r.user_id AND a.id = r.payload ->> 'linkedAccountId') AS linked_account_id,
               left(coalesce(r.payload ->> 'icon', ''), 64) AS icon,
               pg_temp.try_date(r.payload ->> 'deadline') AS deadline,
               coalesce(r.payload ->> 'completed' = 'true', false) AS completed,
               pg_temp.try_instant(r.payload ->> 'completedAt') AS completed_at,
               coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()) AS created_at,
               coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now()) AS updated_at
        FROM public.yourlifeone_records r
        WHERE r.collection = 'savingsGoals' AND r.deleted_at IS NULL AND jsonb_typeof(r.payload) = 'object'
          AND pg_temp.valid_id(r.record_id)
    ) g
    ON CONFLICT DO NOTHING;

    -- The review keeps its document as saved; identity and timestamps become columns.
    INSERT INTO review (user_id, document, as_of, created_at, updated_at)
    SELECT r.user_id,
           r.payload - 'id' - 'createdAt' - 'updatedAt',
           coalesce(pg_temp.try_date(r.payload ->> 'asOf'), (coalesce(r.updated_at, now()) AT TIME ZONE 'UTC')::date),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'financeReviews' AND r.record_id = 'current' AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.pence(jsonb);
DROP FUNCTION pg_temp.valid_id(text);
DROP FUNCTION pg_temp.slug(text, text);
