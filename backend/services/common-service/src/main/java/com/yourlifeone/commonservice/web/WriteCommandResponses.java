package com.yourlifeone.commonservice.web;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.WriteCommandResult;
import org.springframework.http.ResponseEntity;

/** Turns a completed write into its HTTP response, marking replays of an earlier request. */
public final class WriteCommandResponses {
    private WriteCommandResponses() {
    }

    public static <T> ResponseEntity<T> of(WriteCommandResult<T> result) {
        var response = ResponseEntity.status(result.status());
        if (result.replayed()) {
            response.header(IdempotencyKeys.REPLAYED_HEADER, "true");
        }
        return response.body(result.body());
    }
}
