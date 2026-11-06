-- Each user's Google refresh tokens, one per connected Google account. The token is encrypted by the
-- service (AES-256-GCM, key from CALENDAR_GOOGLE_CREDENTIAL_KEY, associated data user_id + email);
-- the database never sees it in plain text. Access tokens are short-lived and kept only in memory.
CREATE TABLE google_credential (
    user_id                  uuid        NOT NULL,
    -- The Google account's address (its primary calendar ID), lower case.
    email                    text        NOT NULL CHECK (length(email) BETWEEN 3 AND 320
                                                         AND email = lower(email)
                                                         AND email ~ '^[^@[:space:]]+@[^@[:space:]]+$'),
    -- Ciphertext plus the 16-byte GCM tag; refresh tokens are at most a few hundred bytes.
    refresh_token_ciphertext bytea       NOT NULL CHECK (octet_length(refresh_token_ciphertext) BETWEEN 17 AND 4112),
    -- A fresh random 12-byte GCM nonce for every write.
    refresh_token_nonce      bytea       NOT NULL CHECK (octet_length(refresh_token_nonce) = 12),
    -- Which configured key encrypted the token.
    key_version              smallint    NOT NULL CHECK (key_version BETWEEN 1 AND 1000),
    scope                    text        CHECK (length(scope) <= 2000),
    last_refresh_at          timestamptz,
    -- Set when Google refused the credential; the user must connect the account again.
    last_failure_reason      text        CHECK (length(last_failure_reason) BETWEEN 1 AND 500),
    last_failure_at          timestamptz,
    revoked_at               timestamptz,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, email),
    CHECK ((last_failure_reason IS NULL) = (last_failure_at IS NULL))
);
