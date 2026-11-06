-- One-time import of the Inventory the React app kept in Supabase's public.yourlifeone_records (collections
-- inventoryItems and inventoryNeeds). A no-op where that table does not exist (tests, fresh databases).
-- Those records were validated when written, so every live one is kept; values that could not be read
-- fall back to the app's defaults.
-- Temporary: once the React app and agents use only this service, yourlifeone_records rows for Inventory are
-- no longer written and this migration never has anything new to import.
CREATE OR REPLACE FUNCTION pg_temp.try_instant(value text) RETURNS timestamptz LANGUAGE plpgsql AS $$
BEGIN
    RETURN date_trunc('milliseconds', value::timestamptz);
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.try_number(value jsonb) RETURNS numeric LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE WHEN jsonb_typeof(value) = 'number' THEN (value #>> '{}')::numeric END;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.text_list(value jsonb, max_count integer, max_length integer) RETURNS text[]
    LANGUAGE sql AS $$
    SELECT coalesce(array_agg(entry ORDER BY first_index), '{}')
    FROM (
        SELECT btrim(element.value) AS entry, min(element.index) AS first_index
        FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(value) = 'array' THEN value ELSE '[]' END)
            WITH ORDINALITY AS element(value, index)
        WHERE length(btrim(element.value)) BETWEEN 1 AND max_length
        GROUP BY btrim(element.value)
        ORDER BY min(element.index)
        LIMIT max_count
    ) entries
$$;

CREATE OR REPLACE FUNCTION pg_temp.https_link(value text) RETURNS text LANGUAGE sql AS $$
    SELECT CASE WHEN length(btrim(value)) <= 2048 AND btrim(value) ~ '^https://[^/@:[:space:]]+' THEN btrim(value) END
$$;

DO $$
BEGIN
    IF to_regclass('public.yourlifeone_records') IS NULL THEN
        RETURN;
    END IF;

    DROP TABLE IF EXISTS pg_temp.imported_record;
    CREATE TEMPORARY TABLE imported_record AS
    SELECT r.user_id, r.collection, r.record_id, r.payload,
           date_trunc('milliseconds', coalesce(r.updated_at, now())) AS record_updated_at,
           CASE WHEN r.payload ->> 'dimensions' IS NOT NULL
                     AND r.payload -> 'dimensions' ->> 'unit' IN ('mm', 'cm', 'm', 'in') THEN r.payload -> 'dimensions'
           END AS dimensions
    FROM public.yourlifeone_records r
    WHERE r.collection IN ('inventoryItems', 'inventoryNeeds')
      AND r.deleted_at IS NULL
      AND jsonb_typeof(r.payload) = 'object'
      AND length(r.record_id) BETWEEN 1 AND 256 AND r.record_id ~ '^[\x21-\x7E]+$';

    INSERT INTO item (user_id, id, name, category, subcategory, image_url, tracking_mode, quantity, unit,
                      low_stock_threshold, brand, model, dimension_length, dimension_width, dimension_height,
                      dimension_unit, condition, location, tags, notes, project_catalog_keys, last_verified_at,
                      archived_at, created_at, updated_at)
    SELECT r.user_id, r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 160), ''), 'Unnamed item'),
           CASE WHEN r.payload ->> 'category' IN ('machine', 'tool', 'electronics', 'component', 'material',
                                                  'consumable', 'fastener', 'safety', 'storage', 'other')
                THEN r.payload ->> 'category' ELSE 'other' END,
           CASE WHEN r.payload ->> 'subcategory' ~ '^[a-z0-9_]{1,40}$' THEN r.payload ->> 'subcategory' END,
           pg_temp.https_link(r.payload ->> 'imageUrl'),
           CASE WHEN r.payload ->> 'trackingMode' IN ('durable', 'counted', 'measured')
                THEN r.payload ->> 'trackingMode' ELSE 'counted' END,
           least(greatest(coalesce(pg_temp.try_number(r.payload -> 'quantity'), 0), 0), 1000000000),
           coalesce(nullif(left(btrim(r.payload ->> 'unit'), 32), ''), 'units'),
           CASE WHEN pg_temp.try_number(r.payload -> 'lowStockThreshold') BETWEEN 0 AND 1000000000
                THEN pg_temp.try_number(r.payload -> 'lowStockThreshold') END,
           nullif(left(btrim(r.payload ->> 'brand'), 120), ''),
           nullif(left(btrim(r.payload ->> 'model'), 120), ''),
           CASE WHEN pg_temp.try_number(r.dimensions -> 'length') > 0 THEN pg_temp.try_number(r.dimensions -> 'length') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'width') > 0 THEN pg_temp.try_number(r.dimensions -> 'width') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'height') > 0 THEN pg_temp.try_number(r.dimensions -> 'height') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'length') > 0 OR pg_temp.try_number(r.dimensions -> 'width') > 0
                     OR pg_temp.try_number(r.dimensions -> 'height') > 0 THEN r.dimensions ->> 'unit' END,
           CASE WHEN r.payload ->> 'condition' IN ('unknown', 'new', 'good', 'worn', 'needs_repair')
                THEN r.payload ->> 'condition' ELSE 'unknown' END,
           nullif(left(btrim(r.payload ->> 'location'), 160), ''),
           pg_temp.text_list(r.payload -> 'tags', 25, 50),
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           pg_temp.text_list(r.payload -> 'projectCatalogKeys', 25, 160),
           coalesce(pg_temp.try_instant(r.payload ->> 'lastVerifiedAt'), pg_temp.try_instant(r.payload ->> 'createdAt'),
                    r.record_updated_at),
           pg_temp.try_instant(r.payload ->> 'archivedAt'),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    WHERE r.collection = 'inventoryItems'
    ON CONFLICT DO NOTHING;

    INSERT INTO need (user_id, id, name, category, subcategory, image_url, linked_item_id, project_catalog_key,
                      required_quantity, unit, dimension_length, dimension_width, dimension_height, dimension_unit,
                      priority, status, notes, ordered_at, acquired_at, dismissed_at, created_at, updated_at)
    SELECT r.user_id, r.record_id,
           coalesce(nullif(left(btrim(r.payload ->> 'name'), 160), ''), 'Unnamed need'),
           category.value,
           CASE WHEN category.value IS NOT NULL AND r.payload ->> 'subcategory' ~ '^[a-z0-9_]{1,40}$'
                THEN r.payload ->> 'subcategory' END,
           pg_temp.https_link(r.payload ->> 'imageUrl'),
           nullif(left(btrim(r.payload ->> 'linkedItemId'), 256), ''),
           nullif(left(btrim(r.payload ->> 'projectCatalogKey'), 160), ''),
           least(greatest(coalesce(pg_temp.try_number(r.payload -> 'requiredQuantity'), 1), 0), 1000000000),
           coalesce(nullif(left(btrim(r.payload ->> 'unit'), 32), ''), 'units'),
           CASE WHEN pg_temp.try_number(r.dimensions -> 'length') > 0 THEN pg_temp.try_number(r.dimensions -> 'length') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'width') > 0 THEN pg_temp.try_number(r.dimensions -> 'width') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'height') > 0 THEN pg_temp.try_number(r.dimensions -> 'height') END,
           CASE WHEN pg_temp.try_number(r.dimensions -> 'length') > 0 OR pg_temp.try_number(r.dimensions -> 'width') > 0
                     OR pg_temp.try_number(r.dimensions -> 'height') > 0 THEN r.dimensions ->> 'unit' END,
           CASE WHEN r.payload ->> 'priority' IN ('low', 'normal', 'high') THEN r.payload ->> 'priority' ELSE 'normal' END,
           CASE WHEN r.payload ->> 'status' IN ('needed', 'ordered', 'acquired', 'dismissed')
                THEN r.payload ->> 'status' ELSE 'needed' END,
           left(coalesce(r.payload ->> 'notes', ''), 4000),
           pg_temp.try_instant(r.payload ->> 'orderedAt'),
           pg_temp.try_instant(r.payload ->> 'acquiredAt'),
           pg_temp.try_instant(r.payload ->> 'dismissedAt'),
           coalesce(pg_temp.try_instant(r.payload ->> 'createdAt'), r.record_updated_at),
           coalesce(pg_temp.try_instant(r.payload ->> 'updatedAt'), r.record_updated_at)
    FROM imported_record r
    CROSS JOIN LATERAL (SELECT CASE WHEN r.payload ->> 'category' IN ('machine', 'tool', 'electronics', 'component',
                                        'material', 'consumable', 'fastener', 'safety', 'storage', 'other')
                                    THEN r.payload ->> 'category' END AS value) category
    WHERE r.collection = 'inventoryNeeds'
    ON CONFLICT DO NOTHING;

    -- Specifications keep at most 30 readable name/value pairs per record.
    INSERT INTO item_specification (user_id, item_id, position, name, value)
    SELECT spec.user_id, spec.record_id, (spec.rank - 1)::integer, spec.name, spec.value
    FROM (
        SELECT r.user_id, r.record_id, btrim(entry.key) AS name, left(btrim(entry.value), 200) AS value,
               row_number() OVER (PARTITION BY r.user_id, r.record_id ORDER BY entry.key) AS rank
        FROM imported_record r
        CROSS JOIN LATERAL jsonb_each_text(CASE WHEN jsonb_typeof(r.payload -> 'specifications') = 'object'
                                                THEN r.payload -> 'specifications' ELSE '{}' END) entry
        WHERE r.collection = 'inventoryItems'
          AND length(btrim(entry.key)) BETWEEN 1 AND 60 AND length(btrim(entry.value)) >= 1
    ) spec
    JOIN item imported ON imported.user_id = spec.user_id AND imported.id = spec.record_id
    WHERE spec.rank <= 30;

    INSERT INTO need_specification (user_id, need_id, position, name, value)
    SELECT spec.user_id, spec.record_id, (spec.rank - 1)::integer, spec.name, spec.value
    FROM (
        SELECT r.user_id, r.record_id, btrim(entry.key) AS name, left(btrim(entry.value), 200) AS value,
               row_number() OVER (PARTITION BY r.user_id, r.record_id ORDER BY entry.key) AS rank
        FROM imported_record r
        CROSS JOIN LATERAL jsonb_each_text(CASE WHEN jsonb_typeof(r.payload -> 'specifications') = 'object'
                                                THEN r.payload -> 'specifications' ELSE '{}' END) entry
        WHERE r.collection = 'inventoryNeeds'
          AND length(btrim(entry.key)) BETWEEN 1 AND 60 AND length(btrim(entry.value)) >= 1
    ) spec
    JOIN need imported ON imported.user_id = spec.user_id AND imported.id = spec.record_id
    WHERE spec.rank <= 30;
    DROP TABLE imported_record;
END $$;

-- Temporary functions live as long as the (pooled) connection; remove them now.
DROP FUNCTION pg_temp.try_instant(text);
DROP FUNCTION pg_temp.try_number(jsonb);
DROP FUNCTION pg_temp.text_list(jsonb, integer, integer);
DROP FUNCTION pg_temp.https_link(text);
