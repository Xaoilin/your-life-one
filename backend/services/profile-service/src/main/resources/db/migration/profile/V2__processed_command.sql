-- Writes arrive through RabbitMQ; each processed command is recorded in the same transaction as its
-- write, so a retried or redelivered command returns the stored result instead of writing again.
CREATE TABLE processed_command (
    message_id      uuid PRIMARY KEY,
    user_id         uuid        NOT NULL,
    command_type    text        NOT NULL,
    -- Digest of the command type and payload: the same idempotency key with another request is refused.
    fingerprint     text        NOT NULL,
    response_status integer,
    response_body   text,
    processed_at    timestamptz NOT NULL DEFAULT now()
);
