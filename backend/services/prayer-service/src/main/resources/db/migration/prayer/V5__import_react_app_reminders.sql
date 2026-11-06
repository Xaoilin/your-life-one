-- One-time import of the reminder receipts the React app kept in Supabase's public.yourlifeone_records
-- (collection prayerTracking), so nothing it already sent is sent again:
--   reminder:<date>::<Prayer>::<deadlineIso>  deadline reminders -> reminder (one per date and prayer; where
--                                              a prayer has several, the latest deadline is kept)
--   bounded:<notificationKey>                  opportunity and momentum receipts -> notice, keeping the key
-- The 'meta' record holds only the schema version. A no-op where public.yourlifeone_records does not exist.
-- Temporary: once the React app no longer writes prayerTracking (the service decides reminders), there is
-- nothing new to import and this migration can be squashed away.
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

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO reminder (user_id, prayer_date, prayer_name, deadline_at, sent_at, snoozed_until, snooze_count,
                          resent_at)
    SELECT DISTINCT ON (receipt.user_id, receipt.prayer_date, receipt.prayer_name)
           receipt.user_id, receipt.prayer_date, receipt.prayer_name, receipt.deadline_at,
           least(receipt.notified_at, receipt.deadline_at),
           receipt.snoozed_until,
           CASE WHEN receipt.snoozed_until IS NULL THEN 0 ELSE 1 END,
           -- A snooze that already ended was shown again by the app.
           CASE WHEN receipt.snoozed_until <= now() THEN receipt.snoozed_until END
    FROM (SELECT r.user_id,
                 pg_temp.try_date(r.payload ->> 'date') AS prayer_date,
                 r.payload ->> 'prayerName' AS prayer_name,
                 pg_temp.try_instant(r.payload ->> 'deadlineAt') AS deadline_at,
                 pg_temp.try_instant(r.payload ->> 'notifiedAt') AS notified_at,
                 pg_temp.try_instant(r.payload ->> 'snoozedUntil') AS snoozed_until
          FROM public.yourlifeone_records r
          WHERE r.collection = 'prayerTracking' AND r.record_id LIKE 'reminder:%' AND r.deleted_at IS NULL
            AND jsonb_typeof(r.payload) = 'object') AS receipt
    WHERE receipt.prayer_date IS NOT NULL AND receipt.deadline_at IS NOT NULL
      AND receipt.prayer_name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')
    ORDER BY receipt.user_id, receipt.prayer_date, receipt.prayer_name, receipt.deadline_at DESC
    ON CONFLICT DO NOTHING;

    INSERT INTO notice (user_id, key, kind, prayer_date, prayer_name, pillar, fires_at, expires_at, sent_at,
                        snoozed_until, snooze_count, resent_at)
    SELECT receipt.user_id, receipt.key, receipt.kind, receipt.prayer_date, receipt.prayer_name, receipt.pillar,
           receipt.fires_at,
           receipt.fires_at + interval '30 minutes',
           coalesce(receipt.notified_at, receipt.attempted_at, receipt.fires_at),
           receipt.snoozed_until,
           CASE WHEN receipt.snoozed_until IS NULL THEN 0 ELSE 1 END,
           CASE WHEN receipt.snoozed_until <= now() THEN receipt.snoozed_until END
    FROM (SELECT r.user_id,
                 substr(r.record_id, length('bounded:') + 1) AS key,
                 CASE WHEN opportunity.parts IS NOT NULL THEN 'prayer-opportunity' ELSE 'momentum' END AS kind,
                 pg_temp.try_date(coalesce(opportunity.parts[1], momentum.parts[1])) AS prayer_date,
                 coalesce(opportunity.parts[2], momentum.parts[3]) AS prayer_name,
                 momentum.parts[2] AS pillar,
                 to_timestamp(coalesce(opportunity.parts[3], momentum.parts[4])::bigint / 1000.0) AS fires_at,
                 pg_temp.try_instant(r.payload ->> 'notifiedAt') AS notified_at,
                 pg_temp.try_instant(r.payload ->> 'attemptedAt') AS attempted_at,
                 pg_temp.try_instant(r.payload ->> 'snoozedUntil') AS snoozed_until
          FROM public.yourlifeone_records r
          LEFT JOIN LATERAL regexp_match(substr(r.record_id, length('bounded:') + 1),
              '^bounded:v1:prayer-opportunity:(\d{4}-\d{2}-\d{2}):(Fajr|Dhuhr|Asr|Maghrib|Isha):(\d{1,15})$')
              AS opportunity(parts) ON true
          LEFT JOIN LATERAL regexp_match(substr(r.record_id, length('bounded:') + 1),
              '^bounded:v1:momentum:(\d{4}-\d{2}-\d{2}):(learn|move):(Fajr|Dhuhr|Asr|Maghrib|Isha):(\d{1,15})$')
              AS momentum(parts) ON true
          WHERE r.collection = 'prayerTracking' AND r.record_id LIKE 'bounded:%' AND r.deleted_at IS NULL
            AND jsonb_typeof(r.payload) = 'object'
            AND (opportunity.parts IS NOT NULL OR momentum.parts IS NOT NULL)) AS receipt
    WHERE receipt.prayer_date IS NOT NULL
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_date(text);
