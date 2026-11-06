-- One-time import of the trips the React app kept in Supabase's public.yourlifeone_records (collections trips,
-- tripLegs, tripItineraryItems, tripBookings and tripBudgetEntries). A no-op where that table does not
-- exist (tests, fresh databases). Parts of a trip that no longer exists are skipped, as the app did;
-- values that could not be read fall back to what the app showed.
-- Temporary: once the React app reads trips only from this service, yourlifeone_records rows for trips are no
-- longer written and this migration never has anything new to import.
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

CREATE OR REPLACE FUNCTION pg_temp.try_amount(value jsonb) RETURNS bigint LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN jsonb_typeof(value) = 'number' AND (value #>> '{}')::numeric BETWEEN 0 AND 100000000000
                THEN round((value #>> '{}')::numeric)::bigint END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.local_time(value text, pattern text, length integer) RETURNS text LANGUAGE sql AS $$
    SELECT CASE WHEN left(value, length) ~ pattern THEN left(value, length) END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    DROP TABLE IF EXISTS pg_temp.imported_record;
    CREATE TEMPORARY TABLE imported_record AS
    SELECT r.user_id, r.collection, r.record_id, r.payload,
           date_trunc('milliseconds', coalesce(r.updated_at, now())) AS record_updated_at
    FROM public.yourlifeone_records r
    WHERE r.collection IN ('trips', 'tripLegs', 'tripItineraryItems', 'tripBookings', 'tripBudgetEntries')
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$';

    INSERT INTO trip (user_id, id, name, summary, notes, status, start_date, end_date, budget_currency, budget_total,
                      created_at, updated_at)
    SELECT r.user_id, r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 160), ''), 'Trip'),
           left(btrim(coalesce(r.payload ->> 'summary', '')), 1000),
           left(coalesce(r.payload ->> 'notes', ''), 10000),
           CASE WHEN r.payload ->> 'status' IN ('planning', 'booked', 'in_trip', 'completed', 'archived')
                THEN r.payload ->> 'status' ELSE 'planning' END,
           dates.start_date,
           CASE WHEN dates.end_date < dates.start_date THEN dates.start_date ELSE coalesce(dates.end_date, dates.start_date) END,
           CASE WHEN upper(btrim(r.payload ->> 'budgetCurrency')) ~ '^[A-Z]{3}$'
                THEN upper(btrim(r.payload ->> 'budgetCurrency')) ELSE 'GBP' END,
           coalesce(pg_temp.try_amount(r.payload -> 'budgetTotal'), 0),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    CROSS JOIN LATERAL (SELECT pg_temp.try_date(r.payload ->> 'startDate') AS start_date,
                               pg_temp.try_date(r.payload ->> 'endDate') AS end_date) dates
    WHERE r.collection = 'trips'
    ON CONFLICT DO NOTHING;

    -- Legs keep their route order, renumbered 0..n-1 per trip.
    INSERT INTO leg (user_id, id, trip_id, country, city, start_date, end_date, sort_order, created_at, updated_at)
    SELECT l.user_id, l.record_id, l.trip_id, l.country, l.city, l.start_date,
           CASE WHEN l.end_date < l.start_date THEN l.start_date ELSE coalesce(l.end_date, l.start_date) END,
           (row_number() OVER (PARTITION BY l.user_id, l.trip_id ORDER BY l.sort_order, l.start_date, l.record_id) - 1)::integer,
           l.created_at, l.updated_at
    FROM (
        SELECT r.user_id, r.record_id, r.payload ->> 'tripId' AS trip_id,
               coalesce(nullif(left(btrim(r.payload ->> 'country'), 120), ''), 'Unknown country') AS country,
               coalesce(nullif(left(btrim(r.payload ->> 'city'), 120), ''), 'Unknown city') AS city,
               pg_temp.try_date(r.payload ->> 'startDate') AS start_date,
               pg_temp.try_date(r.payload ->> 'endDate') AS end_date,
               CASE WHEN jsonb_typeof(r.payload -> 'sortOrder') = 'number' THEN (r.payload ->> 'sortOrder')::numeric END
                   AS sort_order,
               coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at) AS created_at,
               coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at) AS updated_at
        FROM imported_record r
        WHERE r.collection = 'tripLegs'
    ) l
    JOIN trip t ON t.user_id = l.user_id AND t.id = l.trip_id;

    INSERT INTO itinerary_item (user_id, id, trip_id, leg_id, item_date, title, start_time, end_time, location, notes,
                                sort_order, created_at, updated_at)
    SELECT r.user_id, r.record_id, leg.trip_id, leg.id,
           pg_temp.try_date(r.payload ->> 'date'),
           coalesce(nullif(left(btrim(r.payload ->> 'title'), 200), ''), 'Untitled plan'),
           pg_temp.local_time(btrim(r.payload ->> 'startTime'), '^\d{2}:\d{2}$', 5),
           pg_temp.local_time(btrim(r.payload ->> 'endTime'), '^\d{2}:\d{2}$', 5),
           nullif(left(btrim(r.payload ->> 'location'), 200), ''),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           CASE WHEN jsonb_typeof(r.payload -> 'sortOrder') = 'number'
                THEN greatest((r.payload ->> 'sortOrder')::numeric, 0)::integer ELSE 0 END,
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    JOIN leg ON leg.user_id = r.user_id AND leg.id = r.payload ->> 'legId' AND leg.trip_id = r.payload ->> 'tripId'
    WHERE r.collection = 'tripItineraryItems'
    ON CONFLICT DO NOTHING;

    INSERT INTO booking (user_id, id, trip_id, leg_id, kind, title, mode, from_label, to_label, depart_at, arrive_at,
                         property_name, address, city, country, check_in_date, check_out_date, budget_amount,
                         budget_status, budget_date, provider, confirmation_code, link, notes, created_at, updated_at)
    SELECT r.user_id, r.record_id, t.id, leg.id, kind.value,
           coalesce(nullif(left(btrim(r.payload ->> 'title'), 200), ''),
                    CASE kind.value WHEN 'transport' THEN 'Transport booking' ELSE 'Stay booking' END),
           CASE WHEN kind.value <> 'transport' THEN NULL
                WHEN r.payload ->> 'mode' IN ('flight', 'train', 'bus', 'ferry', 'car', 'other') THEN r.payload ->> 'mode'
                ELSE 'other' END,
           CASE WHEN kind.value = 'transport' THEN left(btrim(coalesce(r.payload ->> 'fromLabel', '')), 200) END,
           CASE WHEN kind.value = 'transport' THEN left(btrim(coalesce(r.payload ->> 'toLabel', '')), 200) END,
           CASE WHEN kind.value = 'transport'
                THEN pg_temp.local_time(r.payload ->> 'departAt', '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$', 16) END,
           CASE WHEN kind.value = 'transport'
                THEN coalesce(pg_temp.local_time(r.payload ->> 'arriveAt', '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$', 16),
                              pg_temp.local_time(r.payload ->> 'departAt', '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$', 16)) END,
           CASE WHEN kind.value = 'stay'
                THEN coalesce(nullif(left(btrim(r.payload ->> 'propertyName'), 200), ''), 'Accommodation') END,
           CASE WHEN kind.value = 'stay' THEN nullif(left(btrim(r.payload ->> 'address'), 500), '') END,
           CASE WHEN kind.value = 'stay' THEN left(btrim(coalesce(r.payload ->> 'city', '')), 120) END,
           CASE WHEN kind.value = 'stay' THEN left(btrim(coalesce(r.payload ->> 'country', '')), 120) END,
           CASE WHEN kind.value = 'stay' THEN pg_temp.try_date(r.payload ->> 'checkInDate') END,
           CASE WHEN kind.value = 'stay' THEN coalesce(pg_temp.try_date(r.payload ->> 'checkOutDate'),
                                                        pg_temp.try_date(r.payload ->> 'checkInDate')) END,
           pg_temp.try_amount(r.payload -> 'budgetAmount'),
           CASE WHEN r.payload ->> 'budgetStatus' IN ('planned', 'paid') THEN r.payload ->> 'budgetStatus' END,
           pg_temp.try_date(r.payload ->> 'budgetDate'),
           nullif(left(btrim(r.payload ->> 'provider'), 200), ''),
           nullif(left(btrim(r.payload ->> 'confirmationCode'), 200), ''),
           nullif(left(btrim(r.payload ->> 'link'), 2048), ''),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    CROSS JOIN LATERAL (SELECT CASE WHEN r.payload ->> 'kind' = 'stay' THEN 'stay' ELSE 'transport' END AS value) kind
    JOIN trip t ON t.user_id = r.user_id AND t.id = r.payload ->> 'tripId'
    LEFT JOIN leg ON leg.user_id = r.user_id AND leg.id = r.payload ->> 'legId' AND leg.trip_id = t.id
    WHERE r.collection = 'tripBookings'
    ON CONFLICT DO NOTHING;

    INSERT INTO budget_entry (user_id, id, trip_id, title, category, amount, status, entry_date, notes, created_at,
                              updated_at)
    SELECT r.user_id, r.record_id, t.id,
           coalesce(nullif(left(btrim(r.payload ->> 'title'), 200), ''), 'Budget item'),
           CASE WHEN r.payload ->> 'category' IN ('transport', 'food', 'events', 'rent', 'shopping', 'fees', 'other')
                THEN r.payload ->> 'category' ELSE 'other' END,
           coalesce(pg_temp.try_amount(r.payload -> 'amount'), 0),
           CASE WHEN r.payload ->> 'status' IN ('planned', 'paid') THEN r.payload ->> 'status' ELSE 'planned' END,
           pg_temp.try_date(r.payload ->> 'date'),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    JOIN trip t ON t.user_id = r.user_id AND t.id = r.payload ->> 'tripId'
    WHERE r.collection = 'tripBudgetEntries'
    ON CONFLICT DO NOTHING;
    DROP TABLE imported_record;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_amount(jsonb);
DROP FUNCTION pg_temp.local_time(text, text, integer);
