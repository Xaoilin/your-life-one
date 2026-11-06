package com.yourlifeone.calendar.application.port;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.application.model.ConnectedGoogleAccount;
import com.yourlifeone.calendar.application.model.GoogleTokenGrant;

/**
 * Google's OAuth endpoints, called with this service's OAuth client. Failures are
 * {@link GoogleReconnectRequiredException} when the user must connect the account again, otherwise
 * {@link GoogleUnavailableException}.
 */
public interface GoogleOAuthApi {
    /** Exchanges an authorization code from Google's consent popup. */
    GoogleTokenGrant exchangeCode(String code, String redirectUri);

    /** A new access token for a stored refresh token. */
    GoogleTokenGrant refresh(String refreshToken);

    /** Revokes a token at Google; best effort, never fails. */
    void revoke(String token);

    /** The Google account an access token belongs to, from its primary calendar. */
    ConnectedGoogleAccount account(String accessToken);
}
