package com.yourlifeone.commonservice.command.messaging;

import com.yourlifeone.commonservice.command.WriteCommandReply;
import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/** Builds the replies a consumer sends back. */
final class WriteCommandReplies {
    private WriteCommandReplies() {
    }

    static WriteCommandReply success(HttpStatus status, JsonNode body) {
        return WriteCommandReply.builder().status(status.value()).body(body).build();
    }

    static WriteCommandReply failure(HttpStatus status, String code, String message) {
        return WriteCommandReply.builder().status(status.value()).errorCode(code).errorMessage(message).build();
    }

    static WriteCommandReply failure(ApiException exception) {
        return failure(exception.status(), exception.code(), exception.getMessage());
    }
}
