-- Compatibility tables shared with the independently guarded domain services.
create table if not exists public.yourlifeone_inventory_oauth_clients (
    user_id uuid not null,
    client_id text not null check (client_id = btrim(client_id) and length(client_id) between 1 and 512),
    client_name text not null check (client_name = btrim(client_name) and length(client_name) between 1 and 160),
    approved_at timestamptz not null default now(),
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, client_id)
);
create table if not exists public.yourlifeone_employment_oauth_clients (
    user_id uuid not null,
    client_id text not null check (client_id = btrim(client_id) and length(client_id) between 1 and 512),
    client_name text not null check (client_name = btrim(client_name) and length(client_name) between 1 and 160),
    approved_at timestamptz not null default now(),
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, client_id)
);
create table if not exists public.yourlifeone_equity_oauth_clients (
    user_id uuid not null,
    client_id text not null check (client_id = btrim(client_id) and length(client_id) between 1 and 512),
    client_name text not null check (client_name = btrim(client_name) and length(client_name) between 1 and 160),
    approved_at timestamptz not null default now(),
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, client_id)
);
create table if not exists public.yourlifeone_finance_oauth_clients (
    user_id uuid not null,
    client_id text not null check (client_id = btrim(client_id) and length(client_id) between 1 and 512),
    client_name text not null check (client_name = btrim(client_name) and length(client_name) between 1 and 160),
    approved_at timestamptz not null default now(),
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, client_id)
);
