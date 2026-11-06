package com.yourlifeone.calendar.application.exception;

import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Google refused a change for a reason retrying will not fix, such as a read-only calendar or an event
 * deleted in Google. 409 rather than 403, which the app reserves for its own authorization.
 */
public class GoogleRefusedChangeException extends ApiException {
    public GoogleRefusedChangeException(String message) {
        super(HttpStatus.CONFLICT, "google_refused", message);
    }
}
