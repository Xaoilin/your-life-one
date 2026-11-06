package com.yourlifeone.commonservice.command;

import java.util.UUID;
import lombok.Builder;
import tools.jackson.databind.JsonNode;

/**
 * One write, as published to a service's command queue.
 *
 * @param messageId identifies the write across retries: derived from the caller's idempotency key when given
 * @param type which {@link WriteCommandHandler} performs it
 * @param userId the authenticated caller
 * @param fingerprint digest of the type and payload; the same key with a different request is refused
 * @param payload the handler's input, as JSON
 */
@Builder
public record WriteCommand(UUID messageId, String type, UUID userId, String fingerprint, JsonNode payload) {
}
