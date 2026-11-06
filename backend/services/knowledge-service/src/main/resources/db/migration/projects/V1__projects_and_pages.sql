-- The project catalogue and each project's wiki pages. Links, setup steps, run recipes and the preview
-- are display-only reference documents (never executed), kept as JSON. Allowed statuses and kinds are
-- configuration (app.knowledge.projects.*); these checks bound what is stored.
CREATE TABLE project (
    user_id               uuid        NOT NULL,
    id                    text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    -- Stable catalogue identity that Inventory records link to (projectCatalogKeys).
    catalog_key           text        NOT NULL CHECK (length(catalog_key) BETWEEN 1 AND 300
                                                      AND catalog_key = btrim(catalog_key)),
    name                  text        NOT NULL CHECK (length(name) BETWEEN 1 AND 160 AND name = btrim(name)),
    kind                  text        NOT NULL CHECK (kind ~ '^[a-z][a-z_]{0,31}$'),
    summary               text        NOT NULL DEFAULT '' CHECK (length(summary) <= 4000),
    status                text        NOT NULL CHECK (status ~ '^[a-z][a-z_]{0,31}$'),
    status_before_archive text        CHECK (status_before_archive IS NULL
                                             OR (status = 'archived' AND status_before_archive <> 'archived')),
    is_pinned             boolean     NOT NULL DEFAULT false CHECK (NOT (is_pinned AND status = 'archived')),
    sort_order            integer     NOT NULL CHECK (sort_order >= 0),
    tags                  text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(tags) <= 25),
    links                 jsonb       NOT NULL DEFAULT '[]'
                                      CHECK (jsonb_typeof(links) = 'array' AND jsonb_array_length(links) <= 50),
    setup_steps           jsonb       NOT NULL DEFAULT '[]'
                                      CHECK (jsonb_typeof(setup_steps) = 'array' AND jsonb_array_length(setup_steps) <= 50),
    run_recipes           jsonb       NOT NULL DEFAULT '[]'
                                      CHECK (jsonb_typeof(run_recipes) = 'array' AND jsonb_array_length(run_recipes) <= 20),
    preview               jsonb       NOT NULL CHECK (jsonb_typeof(preview) = 'object'),
    verified_at           timestamptz,
    created_at            timestamptz NOT NULL,
    updated_at            timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    UNIQUE (user_id, catalog_key)
);

-- Deleting a project deletes its pages.
CREATE TABLE page (
    user_id     uuid        NOT NULL,
    id          text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    project_id  text        NOT NULL,
    title       text        NOT NULL CHECK (length(title) BETWEEN 1 AND 200 AND title = btrim(title)),
    content     text        NOT NULL DEFAULT '' CHECK (length(content) <= 200000),
    is_overview boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, project_id) REFERENCES project (user_id, id) ON DELETE CASCADE
);

-- Each project has at most one overview page.
CREATE UNIQUE INDEX page_one_overview ON page (user_id, project_id) WHERE is_overview;
CREATE INDEX page_by_project ON page (user_id, project_id);
