package com.yourlifeone.commonservice.command.persistence;

import java.util.UUID;
import lombok.Builder;

/**
 * A write that has been performed, kept so a retry returns the same result instead of repeating it.
 *
 * @param responseBody JSON response body; null when the response had none
 */
@Builder
public record ProcessedCommand(
        UUID messageId, UUID userId, String commandType, String fingerprint, int responseStatus, String responseBody) {
}
