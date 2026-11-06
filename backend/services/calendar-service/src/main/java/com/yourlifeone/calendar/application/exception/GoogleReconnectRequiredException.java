package com.yourlifeone.calendar.application.exception;

import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Google refused an account's stored credential; the user must connect the account again. Returned
 * as 409, never 401: the user's Your Life One session is fine and must not be treated as signed out.
 */
public class GoogleReconnectRequiredException extends ApiException {
    private final AuthStatus authStatus;

    public GoogleReconnectRequiredException(AuthStatus authStatus, String message) {
        super(HttpStatus.CONFLICT, "google_reconnect_required", message);
        this.authStatus = authStatus;
    }

    /** NEEDS_RECONNECT or REVOKED. */
    public AuthStatus authStatus() {
        return authStatus;
    }
}
