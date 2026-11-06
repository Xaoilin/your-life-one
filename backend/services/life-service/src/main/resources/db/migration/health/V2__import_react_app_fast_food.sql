-- One-time import of the fast-food journal the React app kept in Supabase's public.yourlifeone_records
-- (collection healthFastFoodEntries). A no-op where that table does not exist (tests, fresh databases).
-- Every live entry is kept: unreadable values fall back to what the app showed for them.
-- Temporary: once the React app reads the journal only from this service, yourlifeone_records rows for it are
-- no longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_date(value text) RETURNS date LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN value ~ '^\d{4}-\d{2}-\d{2}$' THEN value::date END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN value::timestamptz;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO fast_food_entry (user_id, id, venue, entry_date, food_order, rating, symptoms, notes,
                                 created_at, updated_at)
    SELECT r.user_id,
           r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'venue'), 160), ''), 'Unknown venue'),
           coalesce(pg_temp.try_date(r.payload ->> 'date'), r.created_at::date, current_date),
           nullif(left(btrim(r.payload ->> 'order'), 500), ''),
           CASE WHEN r.payload ->> 'rating' IN ('good', 'mixed', 'bad', 'awful') THEN r.payload ->> 'rating'
                ELSE 'mixed' END,
           coalesce(ARRAY(SELECT DISTINCT symptom
                          FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(r.payload -> 'symptoms') = 'array'
                                                              THEN r.payload -> 'symptoms' ELSE '[]' END) symptom
                          WHERE symptom IN ('fine', 'bloated', 'sluggish', 'nauseous', 'headache', 'thirsty',
                                            'brain-fog', 'cravings')), '{}'),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.created_at, now()),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.updated_at, now())
    FROM public.yourlifeone_records r
    WHERE r.collection = 'healthFastFoodEntries'
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$'
    ON CONFLICT DO NOTHING;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_date(text);
DROP FUNCTION pg_temp.try_instant(text);
