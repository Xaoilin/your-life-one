-- Private, dated stock and option planning records. Each position is stored whole as the document the
-- app and agents read and replace (validated by the service), with the columns the service queries.
-- Equity never contributes to account balances.
CREATE TABLE position (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    document   jsonb       NOT NULL CHECK (jsonb_typeof(document) = 'object'),
    company    text        NOT NULL CHECK (length(company) BETWEEN 1 AND 200),
    as_of      date        NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);
