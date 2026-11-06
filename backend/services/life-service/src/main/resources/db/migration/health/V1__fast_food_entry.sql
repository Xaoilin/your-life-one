-- The fast-food journal: one row per meal, with how it went and how the user felt after.
-- Allowed ratings and symptoms are configuration (app.life.health.*); these checks bound what is stored.
CREATE TABLE fast_food_entry (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    venue      text        NOT NULL CHECK (length(venue) BETWEEN 1 AND 160 AND venue = btrim(venue)),
    entry_date date        NOT NULL,
    food_order text        CHECK (food_order IS NULL OR length(food_order) BETWEEN 1 AND 500),
    rating     text        NOT NULL CHECK (rating ~ '^[a-z][a-z-]{0,31}$'),
    symptoms   text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(symptoms) <= 20),
    notes      text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE INDEX fast_food_entry_by_date ON fast_food_entry (user_id, entry_date DESC);
