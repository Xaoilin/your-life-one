-- Minimal identity substrate supplied by Supabase in production. Synthetic IDs only.
CREATE SCHEMA auth;
CREATE TABLE auth.users (id uuid PRIMARY KEY);
