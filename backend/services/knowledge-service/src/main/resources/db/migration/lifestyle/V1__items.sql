-- The lifestyle tracker: things to avoid (haram, major sins) and to practise (obligations, halal habits),
-- each in its column, in the order the user arranged. Allowed types and statuses are configuration
-- (app.knowledge.lifestyle.*); these checks bound what is stored.
CREATE TABLE item (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    type       text        NOT NULL CHECK (type ~ '^[a-z][a-z-]{0,31}$'),
    title      text        NOT NULL CHECK (length(title) BETWEEN 1 AND 300 AND title = btrim(title)),
    notes      text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    status     text        NOT NULL CHECK (status ~ '^[a-z][a-z-]{0,31}$'),
    sources    text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(sources) <= 25),
    sort_order integer     NOT NULL CHECK (sort_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE INDEX item_by_column ON item (user_id, type, sort_order);
