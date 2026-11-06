package com.yourlifeone.commonservice.command;

import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Performs one type of write when its command arrives from the queue. Runs in the same database
 * transaction that records the command as processed, so the write happens at most once.
 *
 * @param <P> the command payload, usually the API request body
 */
public interface WriteCommandHandler<P> {
    String type();

    Class<P> payloadType();

    default HttpStatus successStatus() {
        return HttpStatus.OK;
    }

    /** Returns the response body, or null when the response has none. */
    Object handle(UUID userId, P payload);
}
