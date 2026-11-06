-- The manual ledger: accounts, their transactions, monthly budgets and savings goals. Amounts are integer
-- pence. Balances are owned here: a transaction's effect is applied to its accounts when it is saved and
-- reversed when it is replaced or deleted. Allowed types and categories are configuration
-- (app.finance.ledger.*); these checks bound what is stored.
CREATE TABLE account (
    user_id              uuid        NOT NULL,
    id                   text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name                 text        NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    type                 text        NOT NULL CHECK (type ~ '^[a-z][a-z-]{0,31}$'),
    balance              bigint      NOT NULL CHECK (balance BETWEEN -1000000000000000 AND 1000000000000000),
    currency             text        NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    color                text        NOT NULL CHECK (length(color) <= 64),
    icon                 text        NOT NULL CHECK (length(icon) <= 64),
    include_in_net_worth boolean     NOT NULL,
    sort_order           integer     NOT NULL CHECK (sort_order >= 0),
    created_at           timestamptz NOT NULL,
    updated_at           timestamptz NOT NULL,
    PRIMARY KEY (user_id, id)
);

-- A transfer moves money from its account to another of the same user's accounts; income and expense
-- touch one account. Deleting an account deletes every transaction on either side of it.
CREATE TABLE transaction (
    user_id       uuid        NOT NULL,
    id            text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    type          text        NOT NULL CHECK (type IN ('income', 'expense', 'transfer')),
    -- Zero only for records imported from the React app, which allowed it; new writes need a positive amount.
    amount        bigint      NOT NULL CHECK (amount BETWEEN 0 AND 1000000000000),
    category      text        NOT NULL CHECK (category ~ '^[a-z][a-z-]{0,63}$'),
    account_id    text        NOT NULL,
    to_account_id text,
    description   text        NOT NULL CHECK (length(description) <= 4000),
    date          date        NOT NULL,
    tags          text[]      NOT NULL DEFAULT '{}' CHECK (cardinality(tags) <= 20),
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, account_id) REFERENCES account (user_id, id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, to_account_id) REFERENCES account (user_id, id) ON DELETE CASCADE,
    CONSTRAINT transaction_transfer_has_other_account CHECK (
        (type = 'transfer') = (to_account_id IS NOT NULL) AND to_account_id IS DISTINCT FROM account_id)
);

CREATE INDEX transaction_account ON transaction (user_id, account_id);
CREATE INDEX transaction_to_account ON transaction (user_id, to_account_id) WHERE to_account_id IS NOT NULL;

-- One monthly budget per expense category.
CREATE TABLE budget (
    user_id       uuid        NOT NULL,
    id            text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    category      text        NOT NULL CHECK (category ~ '^[a-z][a-z-]{0,63}$'),
    monthly_limit bigint      NOT NULL CHECK (monthly_limit BETWEEN 0 AND 1000000000000),
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT budget_one_per_category UNIQUE (user_id, category)
);

-- A goal may point at the account it is saved in; deleting that account clears the link (the service
-- does it in the same transaction).
CREATE TABLE savings_goal (
    user_id           uuid        NOT NULL,
    id                text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name              text        NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    target_amount     bigint      NOT NULL CHECK (target_amount BETWEEN 0 AND 1000000000000),
    current_amount    bigint      NOT NULL CHECK (current_amount BETWEEN 0 AND 1000000000000),
    linked_account_id text        CHECK (length(linked_account_id) BETWEEN 1 AND 256),
    icon              text        NOT NULL CHECK (length(icon) <= 64),
    deadline          date,
    completed         boolean     NOT NULL,
    completed_at      timestamptz,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT savings_goal_completed_has_time CHECK (completed = (completed_at IS NOT NULL))
);

-- The dated banking review: one per user, stored whole as the document the app and agents read and
-- replace (validated by the service), with the columns the service queries.
CREATE TABLE review (
    user_id    uuid        PRIMARY KEY,
    document   jsonb       NOT NULL CHECK (jsonb_typeof(document) = 'object'),
    as_of      date        NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
