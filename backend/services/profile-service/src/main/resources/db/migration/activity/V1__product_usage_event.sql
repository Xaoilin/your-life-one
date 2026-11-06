-- The React app's content-free product-usage events, mirroring Supabase's public.product_usage_events.
-- Allowed kinds, surfaces, outcomes, device classes, input kinds and metadata keys are configuration
-- (app.profile.activity.ingest.*) and checked before a write; these checks bound what can be stored.
-- A repeated event, or a second event at the same place in a session, is a duplicate: writes use
-- ON CONFLICT DO NOTHING, so neither unique constraint may be DEFERRABLE.

-- Metadata is a small object of scalar values; CHECK constraints cannot run subqueries, hence a function.
CREATE FUNCTION metadata_is_bounded(metadata jsonb) RETURNS boolean
    LANGUAGE sql IMMUTABLE SET search_path = '' AS $$
    SELECT jsonb_typeof(metadata) = 'object'
       AND octet_length(metadata::text) <= 2048
       AND NOT EXISTS (
           SELECT 1
           FROM jsonb_each(metadata) AS entry(key, value)
           WHERE length(entry.key) > 64
              OR jsonb_typeof(entry.value) NOT IN ('string', 'number', 'boolean', 'null')
              OR (jsonb_typeof(entry.value) = 'string' AND length(entry.value #>> '{}') > 96))
$$;

CREATE TABLE product_usage_event (
    user_id         uuid        NOT NULL,
    event_id        uuid        NOT NULL,
    schema_version  smallint    NOT NULL CHECK (schema_version = 1),
    session_id      uuid        NOT NULL,
    sequence        integer     NOT NULL CHECK (sequence BETWEEN 1 AND 1000000),
    event_kind      text        NOT NULL CHECK (event_kind ~ '^[a-z][a-z_]{0,31}$'),
    occurred_at     timestamptz NOT NULL,
    surface         text        CHECK (surface IS NULL OR surface ~ '^[a-z][a-z_-]{0,31}$'),
    feature         text        NOT NULL CHECK (feature ~ '^[a-z][a-z0-9_]{0,63}$'),
    action          text        NOT NULL CHECK (action ~ '^[a-z][a-z0-9_]{0,63}$'),
    outcome         text        CHECK (outcome IS NULL OR outcome ~ '^[a-z][a-z_]{0,31}$'),
    duration_ms     integer     CHECK (duration_ms IS NULL OR duration_ms BETWEEN 0 AND 1800000),
    error_code      text        CHECK (error_code IS NULL OR error_code ~ '^[a-z][a-z0-9_:-]{0,63}$'),
    target          text        CHECK (target IS NULL OR target ~ '^[a-z][a-z0-9_:-]{0,95}$'),
    release_version text        NOT NULL CHECK (length(release_version) <= 32
                                    AND release_version ~ '^[0-9]+\.[0-9]+\.[0-9]+([+-][a-zA-Z0-9.-]+)?$'),
    device_class    text        NOT NULL CHECK (device_class ~ '^[a-z][a-z_]{0,31}$'),
    input_kind      text        NOT NULL CHECK (input_kind ~ '^[a-z][a-z_]{0,31}$'),
    online          boolean     NOT NULL,
    reduced_motion  boolean     NOT NULL,
    metadata        jsonb       NOT NULL DEFAULT '{}' CHECK (metadata_is_bounded(metadata)),
    received_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT product_usage_event_owner_event_unique UNIQUE (user_id, event_id),
    CONSTRAINT product_usage_event_owner_session_sequence_unique UNIQUE (user_id, session_id, sequence)
);

CREATE INDEX product_usage_event_by_time ON product_usage_event (user_id, occurred_at DESC);
