-- Trips: each trip has route legs (in order), day plans on those legs, transport and stay bookings, and
-- manual budget items. Deleting a trip deletes its parts; deleting a leg deletes its plans and bookings.
-- Amounts are minor units (pence, cents). Allowed values are configuration (app.life.trips.*); these
-- checks bound what is stored.
CREATE TABLE trip (
    user_id         uuid        NOT NULL,
    id              text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    name            text        NOT NULL CHECK (length(name) BETWEEN 1 AND 160 AND name = btrim(name)),
    summary         text        NOT NULL DEFAULT '' CHECK (length(summary) <= 1000),
    notes           text        NOT NULL DEFAULT '' CHECK (length(notes) <= 10000),
    status          text        NOT NULL CHECK (status ~ '^[a-z_]{1,32}$'),
    start_date      date,
    end_date        date,
    budget_currency text        NOT NULL CHECK (budget_currency ~ '^[A-Z]{3}$'),
    budget_total    bigint      NOT NULL DEFAULT 0 CHECK (budget_total BETWEEN 0 AND 100000000000),
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT trip_dates_in_order CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE leg (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    trip_id    text        NOT NULL,
    country    text        NOT NULL CHECK (length(country) BETWEEN 1 AND 120 AND country = btrim(country)),
    city       text        NOT NULL CHECK (length(city) BETWEEN 1 AND 120 AND city = btrim(city)),
    start_date date,
    end_date   date,
    sort_order integer     NOT NULL CHECK (sort_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, trip_id) REFERENCES trip (user_id, id) ON DELETE CASCADE,
    -- Checked at commit: moving a leg swaps two positions within one save.
    CONSTRAINT leg_position_once UNIQUE (user_id, trip_id, sort_order) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT leg_dates_in_order CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE itinerary_item (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    trip_id    text        NOT NULL,
    leg_id     text        NOT NULL,
    item_date  date,
    title      text        NOT NULL CHECK (length(title) BETWEEN 1 AND 200 AND title = btrim(title)),
    start_time text        CHECK (start_time IS NULL OR start_time ~ '^\d{2}:\d{2}$'),
    end_time   text        CHECK (end_time IS NULL OR end_time ~ '^\d{2}:\d{2}$'),
    location   text        CHECK (location IS NULL OR length(location) BETWEEN 1 AND 200),
    notes      text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    sort_order integer     NOT NULL CHECK (sort_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, trip_id) REFERENCES trip (user_id, id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, leg_id) REFERENCES leg (user_id, id) ON DELETE CASCADE
);

CREATE TABLE booking (
    user_id           uuid        NOT NULL,
    id                text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    trip_id           text        NOT NULL,
    leg_id            text,
    kind              text        NOT NULL CHECK (kind IN ('transport', 'stay')),
    title             text        NOT NULL CHECK (length(title) BETWEEN 1 AND 200 AND title = btrim(title)),
    mode              text        CHECK (mode IS NULL OR mode ~ '^[a-z_]{1,32}$'),
    from_label        text        CHECK (from_label IS NULL OR length(from_label) <= 200),
    to_label          text        CHECK (to_label IS NULL OR length(to_label) <= 200),
    -- Local wall-clock times as the traveller sees them, YYYY-MM-DDTHH:MM.
    depart_at         text        CHECK (depart_at IS NULL OR depart_at ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$'),
    arrive_at         text        CHECK (arrive_at IS NULL OR arrive_at ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$'),
    property_name     text        CHECK (property_name IS NULL OR length(property_name) BETWEEN 1 AND 200),
    address           text        CHECK (address IS NULL OR length(address) BETWEEN 1 AND 500),
    city              text        CHECK (city IS NULL OR length(city) <= 120),
    country           text        CHECK (country IS NULL OR length(country) <= 120),
    check_in_date     date,
    check_out_date    date,
    budget_amount     bigint      CHECK (budget_amount IS NULL OR budget_amount BETWEEN 0 AND 100000000000),
    budget_status     text        CHECK (budget_status IS NULL OR budget_status ~ '^[a-z_]{1,32}$'),
    budget_date       date,
    provider          text        CHECK (provider IS NULL OR length(provider) BETWEEN 1 AND 200),
    confirmation_code text        CHECK (confirmation_code IS NULL OR length(confirmation_code) BETWEEN 1 AND 200),
    link              text        CHECK (link IS NULL OR length(link) BETWEEN 1 AND 2048),
    notes             text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, trip_id) REFERENCES trip (user_id, id) ON DELETE CASCADE,
    FOREIGN KEY (user_id, leg_id) REFERENCES leg (user_id, id) ON DELETE CASCADE,
    -- Transport has a mode and route; a stay has a property.
    CONSTRAINT booking_kind_fields CHECK ((kind = 'transport' AND mode IS NOT NULL AND property_name IS NULL)
        OR (kind = 'stay' AND mode IS NULL AND property_name IS NOT NULL))
);

CREATE TABLE budget_entry (
    user_id    uuid        NOT NULL,
    id         text        NOT NULL CHECK (length(id) BETWEEN 1 AND 256 AND id ~ '^[\x21-\x7E]+$'),
    trip_id    text        NOT NULL,
    title      text        NOT NULL CHECK (length(title) BETWEEN 1 AND 200 AND title = btrim(title)),
    category   text        NOT NULL CHECK (category ~ '^[a-z_]{1,32}$'),
    amount     bigint      NOT NULL CHECK (amount BETWEEN 0 AND 100000000000),
    status     text        NOT NULL CHECK (status ~ '^[a-z_]{1,32}$'),
    entry_date date,
    notes      text        NOT NULL DEFAULT '' CHECK (length(notes) <= 4000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, id),
    FOREIGN KEY (user_id, trip_id) REFERENCES trip (user_id, id) ON DELETE CASCADE
);
