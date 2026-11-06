package com.yourlifeone.commonservice.command;

import lombok.Builder;
import tools.jackson.databind.JsonNode;

/**
 * The outcome of one write, sent back to the waiting API request.
 *
 * @param status HTTP status for the caller
 * @param body response body on success; null when there is none
 * @param errorCode {@code ApiErrorResponse} code on failure
 * @param errorMessage {@code ApiErrorResponse} message on failure
 */
@Builder
public record WriteCommandReply(int status, JsonNode body, String errorCode, String errorMessage) {
    public boolean failed() {
        return status >= 400;
    }
}
