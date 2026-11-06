-- One-time import of the job tracker the React app kept in Supabase's public.yourlifeone_records (collection
-- employment, one "singleton" record holding every application). A no-op where that table does not
-- exist (tests, fresh databases). Every application and history entry is kept in its order; values the
-- app could not have shown fall back to its defaults, and a repeated evidence link is kept once.
-- Temporary: once the React app and agents use only this service, the singleton is no longer written
-- and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_date(value text) RETURNS date LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN value ~ '^\d{4}-\d{2}-\d{2}$' THEN value::date END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN date_trunc('milliseconds', value::timestamptz);
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.web_link(value text) RETURNS text LANGUAGE sql AS $$
    SELECT CASE WHEN length(btrim(value)) <= 2048 AND btrim(value) ~ '^https?://[^[:space:]/]+[^[:space:]]*$'
                     AND btrim(value) !~ '^https?://[^/]*@' THEN btrim(value) END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    DROP TABLE IF EXISTS pg_temp.imported_application;
    CREATE TEMPORARY TABLE imported_application AS
    SELECT DISTINCT ON (r.user_id, app.value ->> 'id')
           r.user_id, app.value AS payload, (app.ordinality - 1)::integer AS position,
           r.updated_at AS record_updated_at
    FROM public.yourlifeone_records r
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(r.payload -> 'applications') = 'array'
                                                 THEN r.payload -> 'applications' ELSE '[]' END)
        WITH ORDINALITY AS app(value, ordinality)
    WHERE r.collection = 'employment'
      AND r.record_id = 'singleton'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(app.value) = 'object'
      AND length(app.value ->> 'id') BETWEEN 1 AND 256 AND app.value ->> 'id' ~ '^[\x21-\x7E]+$'
    ORDER BY r.user_id, app.value ->> 'id', app.ordinality;

    INSERT INTO application (user_id, id, position, company, role, url, work_type, remote_region, remote_status,
                             remote_evidence, remote_caveat, compensation, status, application_date, next_action,
                             next_action_date, notes, created_at, updated_at)
    SELECT a.user_id,
           a.payload ->> 'id',
           a.position,
           coalesce(nullif(left(btrim(a.payload ->> 'company'), 256), ''), 'Unknown company'),
           coalesce(nullif(left(btrim(a.payload ->> 'role'), 256), ''), 'Unknown role'),
           pg_temp.web_link(a.payload ->> 'url'),
           CASE WHEN a.payload ->> 'workType' IN ('contract', 'permanent', 'unknown') THEN a.payload ->> 'workType'
                ELSE 'unknown' END,
           region.value,
           CASE WHEN a.payload ->> 'remoteStatus' = 'confirmed' AND region.value <> 'unknown' THEN 'confirmed'
                ELSE 'needs_verification' END,
           coalesce(nullif(left(btrim(a.payload ->> 'remoteEvidence'), 10000), ''),
                    'Remote eligibility needs verification.'),
           nullif(left(a.payload ->> 'remoteCaveat', 2000), ''),
           nullif(left(a.payload ->> 'compensation', 2000), ''),
           CASE WHEN a.payload ->> 'status' IN ('lead', 'recruiter', 'applied', 'interview', 'offer', 'closed')
                THEN a.payload ->> 'status' ELSE 'lead' END,
           pg_temp.try_date(a.payload ->> 'applicationDate'),
           coalesce(nullif(left(btrim(a.payload ->> 'nextAction'), 2000), ''),
                    'Review the latest evidence and decide the next step.'),
           pg_temp.try_date(a.payload ->> 'nextActionDate'),
           left(coalesce(a.payload ->> 'notes', ''), 10000),
           coalesce(pg_temp.try_instant(a.payload ->> 'createdAt'), date_trunc('milliseconds', a.record_updated_at),
                    date_trunc('milliseconds', now())),
           coalesce(pg_temp.try_instant(a.payload ->> 'updatedAt'), date_trunc('milliseconds', a.record_updated_at),
                    date_trunc('milliseconds', now()))
    FROM imported_application a
    CROSS JOIN LATERAL (SELECT CASE WHEN a.payload ->> 'remoteRegion' IN ('uk', 'emea', 'global', 'unknown')
                                    THEN a.payload ->> 'remoteRegion' ELSE 'unknown' END AS value) region
    ON CONFLICT DO NOTHING;

    -- History keeps its order, numbered without gaps; an entry without an ID gets one, and a repeated ID or
    -- evidence link is kept once (its first occurrence).
    INSERT INTO history_entry (user_id, application_id, position, id, kind, entry_date, summary, details, evidence_url)
    SELECT ranked.user_id, ranked.application_id,
           (row_number() OVER (PARTITION BY ranked.user_id, ranked.application_id ORDER BY ranked.ordinality) - 1)::integer,
           ranked.id, ranked.kind, ranked.entry_date, ranked.summary, ranked.details, ranked.evidence_url
    FROM (
        SELECT entry.*,
               row_number() OVER (PARTITION BY entry.user_id, entry.application_id, entry.id
                                  ORDER BY entry.ordinality) AS id_rank,
               row_number() OVER (PARTITION BY entry.user_id, entry.application_id, entry.evidence_url
                                  ORDER BY entry.ordinality) AS link_rank
        FROM (
            SELECT a.user_id,
                   a.payload ->> 'id' AS application_id,
                   history.ordinality,
                   left(coalesce(nullif(btrim(history.value ->> 'id'), ''), 'history-' || history.ordinality), 256) AS id,
                   CASE WHEN history.value ->> 'kind' IN ('application', 'contact', 'document', 'remote_evidence', 'note')
                        THEN history.value ->> 'kind' ELSE 'note' END AS kind,
                   pg_temp.try_date(history.value ->> 'date') AS entry_date,
                   coalesce(nullif(left(btrim(history.value ->> 'summary'), 2000), ''), 'Recorded update') AS summary,
                   left(coalesce(history.value ->> 'details', ''), 10000) AS details,
                   pg_temp.web_link(history.value ->> 'evidenceUrl') AS evidence_url
            FROM imported_application a
            CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(a.payload -> 'history') = 'array'
                                                         THEN a.payload -> 'history' ELSE '[]' END)
                WITH ORDINALITY AS history(value, ordinality)
            WHERE jsonb_typeof(history.value) = 'object'
        ) entry
        JOIN application imported ON imported.user_id = entry.user_id AND imported.id = entry.application_id
    ) ranked
    WHERE ranked.id_rank = 1 AND (ranked.evidence_url IS NULL OR ranked.link_rank = 1)
    ON CONFLICT DO NOTHING;
    DROP TABLE imported_application;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.web_link(text);
