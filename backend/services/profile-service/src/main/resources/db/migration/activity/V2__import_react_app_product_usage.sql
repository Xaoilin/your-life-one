-- One-time import of the product-usage events the React app sent to Supabase's
-- public.product_usage_events. A no-op where that table does not exist (tests, fresh databases).
-- Supabase checked every row against the same rules on the way in, so each is copied as it is;
-- a row already here is skipped.
-- Temporary: once the React app sends events only to this service, Supabase's ingest function is
-- retired, the table stops growing and this migration never has anything new to import.
DO $$
BEGIN
    IF to_regclass('public.product_usage_events') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO product_usage_event (user_id, event_id, schema_version, session_id, sequence, event_kind,
                                     occurred_at, surface, feature, action, outcome, duration_ms, error_code,
                                     target, release_version, device_class, input_kind, online, reduced_motion,
                                     metadata, received_at)
    SELECT e.user_id, e.event_id, e.schema_version, e.session_id, e.sequence, e.event_kind,
           e.occurred_at, e.surface, e.feature, e.action, e.outcome, e.duration_ms, e.error_code,
           e.target, e.release_version, e.device_class, e.input_kind, e.online, e.reduced_motion,
           coalesce(e.metadata, '{}'), e.received_at
    FROM public.product_usage_events e
    ON CONFLICT DO NOTHING;
END $$;
