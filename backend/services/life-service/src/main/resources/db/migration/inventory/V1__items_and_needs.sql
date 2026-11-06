-- Live Inventory is the authoritative stock: owned items (archived, never deleted) and needs (what to
-- buy, from needed to ordered to acquired or dismissed). Categories group records into broad sections;
-- subcategories are metadata. Dimensions are an optional envelope (length, width, height, unit);
-- everything else stays in specifications. Allowed values are configuration (app.life.inventory.*);
-- these checks bound what is stored.
CREATE TABLE item (
    user_id             uuid          NOT NULL,
    id                  text          NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name                text          NOT NULL CHECK (length(name) BETWEEN 1 AND 160 AND name = btrim(name)),
    category            text          NOT NULL CHECK (category ~ '^[a-z0-9_]{1,40}$'),
    subcategory         text          CHECK (subcategory IS NULL OR subcategory ~ '^[a-z0-9_]{1,40}$'),
    image_url           text          CHECK (image_url IS NULL OR (length(image_url) <= 2048 AND image_url ~ '^https://')),
    tracking_mode       text          NOT NULL CHECK (tracking_mode ~ '^[a-z_]{1,32}$'),
    quantity            numeric       NOT NULL CHECK (quantity BETWEEN 0 AND 1000000000),
    unit                text          NOT NULL CHECK (length(unit) BETWEEN 1 AND 32 AND unit = btrim(unit)),
    low_stock_threshold numeric       CHECK (low_stock_threshold IS NULL OR low_stock_threshold BETWEEN 0 AND 1000000000),
    brand               text          CHECK (brand IS NULL OR length(brand) BETWEEN 1 AND 120),
    model               text          CHECK (model IS NULL OR length(model) BETWEEN 1 AND 120),
    dimension_length    numeric       CHECK (dimension_length IS NULL OR dimension_length > 0),
    dimension_width     numeric       CHECK (dimension_width IS NULL OR dimension_width > 0),
    dimension_height    numeric       CHECK (dimension_height IS NULL OR dimension_height > 0),
    dimension_unit      text          CHECK (dimension_unit IS NULL OR dimension_unit ~ '^[a-z]{1,8}$'),
    condition           text          NOT NULL CHECK (condition ~ '^[a-z_]{1,32}$'),
    location            text          CHECK (location IS NULL OR length(location) BETWEEN 1 AND 160),
    tags                text[]        NOT NULL DEFAULT '{}' CHECK (cardinality(tags) <= 25),
    notes               text          NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    project_catalog_keys text[]       NOT NULL DEFAULT '{}' CHECK (cardinality(project_catalog_keys) <= 25),
    last_verified_at    timestamptz   NOT NULL,
    archived_at         timestamptz,
    created_at          timestamptz   NOT NULL,
    updated_at          timestamptz   NOT NULL,
    PRIMARY KEY (user_id, id),
    -- A unit comes with at least one axis, and an axis needs its unit.
    CONSTRAINT item_dimensions_complete CHECK ((dimension_unit IS NULL) = (dimension_length IS NULL
        AND dimension_width IS NULL AND dimension_height IS NULL))
);

CREATE TABLE item_specification (
    user_id  uuid    NOT NULL,
    item_id  text    NOT NULL,
    position integer NOT NULL CHECK (position BETWEEN 0 AND 29),
    name     text    NOT NULL CHECK (length(name) BETWEEN 1 AND 60 AND name = btrim(name)),
    value    text    NOT NULL CHECK (length(value) BETWEEN 1 AND 200 AND value = btrim(value)),
    PRIMARY KEY (user_id, item_id, position),
    FOREIGN KEY (user_id, item_id) REFERENCES item (user_id, id) ON DELETE CASCADE,
    -- Checked at commit: a save may move names between positions.
    CONSTRAINT item_specification_name_once UNIQUE (user_id, item_id, name) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE need (
    user_id             uuid        NOT NULL,
    id                  text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name                text        NOT NULL CHECK (length(name) BETWEEN 1 AND 160 AND name = btrim(name)),
    category            text        CHECK (category IS NULL OR category ~ '^[a-z0-9_]{1,40}$'),
    subcategory         text        CHECK (subcategory IS NULL OR subcategory ~ '^[a-z0-9_]{1,40}$'),
    image_url           text        CHECK (image_url IS NULL OR (length(image_url) <= 2048 AND image_url ~ '^https://')),
    linked_item_id      text        CHECK (linked_item_id IS NULL OR length(linked_item_id) BETWEEN 1 AND 256),
    project_catalog_key text        CHECK (project_catalog_key IS NULL OR length(project_catalog_key) BETWEEN 1 AND 160),
    required_quantity   numeric     NOT NULL CHECK (required_quantity BETWEEN 0 AND 1000000000),
    unit                text        NOT NULL CHECK (length(unit) BETWEEN 1 AND 32 AND unit = btrim(unit)),
    dimension_length    numeric     CHECK (dimension_length IS NULL OR dimension_length > 0),
    dimension_width     numeric     CHECK (dimension_width IS NULL OR dimension_width > 0),
    dimension_height    numeric     CHECK (dimension_height IS NULL OR dimension_height > 0),
    dimension_unit      text        CHECK (dimension_unit IS NULL OR dimension_unit ~ '^[a-z]{1,8}$'),
    priority            text        NOT NULL CHECK (priority ~ '^[a-z_]{1,32}$'),
    status              text        NOT NULL CHECK (status ~ '^[a-z_]{1,32}$'),
    notes               text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    ordered_at          timestamptz,
    acquired_at         timestamptz,
    dismissed_at        timestamptz,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    -- A subcategory belongs to a category.
    CONSTRAINT need_subcategory_has_category CHECK (subcategory IS NULL OR category IS NOT NULL),
    CONSTRAINT need_dimensions_complete CHECK ((dimension_unit IS NULL) = (dimension_length IS NULL
        AND dimension_width IS NULL AND dimension_height IS NULL))
);

CREATE TABLE need_specification (
    user_id  uuid    NOT NULL,
    need_id  text    NOT NULL,
    position integer NOT NULL CHECK (position BETWEEN 0 AND 29),
    name     text    NOT NULL CHECK (length(name) BETWEEN 1 AND 60 AND name = btrim(name)),
    value    text    NOT NULL CHECK (length(value) BETWEEN 1 AND 200 AND value = btrim(value)),
    PRIMARY KEY (user_id, need_id, position),
    FOREIGN KEY (user_id, need_id) REFERENCES need (user_id, id) ON DELETE CASCADE,
    CONSTRAINT need_specification_name_once UNIQUE (user_id, need_id, name) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX need_open_by_category ON need (user_id, category) WHERE status IN ('needed', 'ordered');
