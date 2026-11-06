package com.yourlifeone.calendar.application.exception;

import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;

/** Google failed, or its OAuth client is misconfigured, or is rate limiting; the same request can be retried. */
public class GoogleUnavailableException extends ApiException {
    public GoogleUnavailableException(String detail, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "google_unavailable",
                "Google Calendar is unavailable right now; try again shortly.", detail, cause);
    }
}
