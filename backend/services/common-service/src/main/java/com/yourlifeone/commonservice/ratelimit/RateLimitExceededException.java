package com.yourlifeone.commonservice.ratelimit;

import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;

/** A user made too many requests; answered 429 with a {@code Retry-After} header. */
public class RateLimitExceededException extends ApiException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                "Too many requests. Try again after the Retry-After delay.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
