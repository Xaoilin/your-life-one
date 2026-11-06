-- The knowledge base: topics, and entries (notes with Quran, hadith and other sources) inside them.
-- Allowed source types are configuration (app.knowledge.knowledge.*); these checks bound what is stored.
CREATE TABLE topic (
    user_id     uuid        NOT NULL,
    id          text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name        text        NOT NULL CHECK (length(name) BETWEEN 1 AND 160 AND name = btrim(name)),
    description text        NOT NULL DEFAULT '' CHECK (length(description) <= 4000),
    icon        text        NOT NULL DEFAULT '' CHECK (length(icon) <= 40),
    color       text        NOT NULL DEFAULT '' CHECK (length(color) <= 32),
    sort_order  integer     NOT NULL CHECK (sort_order >= 0),
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);

-- Deleting a topic deletes its entries: the topic is the one record the user chose to delete.
CREATE TABLE entry (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    topic_id   text        NOT NULL,
    title      text        NOT NULL CHECK (length(title) BETWEEN 1 AND 300 AND title = btrim(title)),
    content    text        NOT NULL DEFAULT '' CHECK (length(content) <= 100000),
    sources    jsonb       NOT NULL DEFAULT '[]'
                           CHECK (jsonb_typeof(sources) = 'array' AND jsonb_array_length(sources) <= 50),
    tags       text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(tags) <= 25),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, topic_id) REFERENCES topic (user_id, id) ON DELETE CASCADE
);

CREATE INDEX entry_by_topic ON entry (user_id, topic_id);
