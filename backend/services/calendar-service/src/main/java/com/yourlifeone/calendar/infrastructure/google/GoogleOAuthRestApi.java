package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.application.model.ConnectedGoogleAccount;
import com.yourlifeone.calendar.application.model.GoogleTokenGrant;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.config.GoogleOAuthProperties;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleCalendarListEntry;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleCalendarListResponse;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleTokenResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Google's OAuth 2.0 endpoints for web server apps
 * (https://developers.google.com/identity/protocols/oauth2/web-server), plus the calendar list that
 * identifies the account. Tokens never appear in exceptions or logs.
 */
public class GoogleOAuthRestApi implements GoogleOAuthApi {
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthRestApi.class);

    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String CODE = "code";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String TOKEN = "token";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final Set<String> CLIENT_MISCONFIGURED = Set.of("invalid_client", "unauthorized_client");
    private static final String UNKNOWN_ERROR = "unknown_error";
    private static final String CALENDAR_LIST_PATH = "/users/me/calendarList";
    private static final String MIN_ACCESS_ROLE = "minAccessRole";
    private static final String READER = "reader";
    private static final String OWNER = "owner";

    private static final String CODE_REFUSED = "Google did not accept the sign-in. Connect the account again.";
    private static final String REFRESH_REFUSED = "Google refresh token expired or was revoked. Reconnect this account.";
    private static final String ACCESS_EXPIRED = "Google access expired. Reconnect this account.";
    private static final String ACCESS_REVOKED = "Google access was revoked. Reconnect this account.";

    private final RestClient oauth;
    private final RestClient calendar;
    private final GoogleOAuthProperties properties;
    private final Clock clock;

    /**
     * @param oauth a client for the absolute token and revoke URLs
     * @param calendar a client whose base URL is the Calendar API root
     */
    public GoogleOAuthRestApi(RestClient oauth, RestClient calendar, GoogleOAuthProperties properties, Clock clock) {
        this.oauth = oauth;
        this.calendar = calendar;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public GoogleTokenGrant exchangeCode(String code, String redirectUri) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add(GRANT_TYPE, AUTHORIZATION_CODE);
        form.add(CODE, code);
        form.add(REDIRECT_URI, redirectUri);
        return token("code exchange", form, CODE_REFUSED);
    }

    @Override
    public GoogleTokenGrant refresh(String refreshToken) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add(GRANT_TYPE, REFRESH_TOKEN);
        form.add(REFRESH_TOKEN, refreshToken);
        return token("token refresh", form, REFRESH_REFUSED);
    }

    @Override
    public void revoke(String token) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add(TOKEN, token);
        try {
            oauth.post().uri(properties.revokeUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            logger.warn("Google did not revoke a token: status={}", exception.getStatusCode().value());
        } catch (RestClientException exception) {
            logger.warn("Google token revocation failed: {}", exception.getClass().getSimpleName());
        }
    }

    @Override
    public ConnectedGoogleAccount account(String accessToken) {
        var primary = primaryCalendar(calendars(accessToken))
                .filter(entry -> entry.id() != null && !entry.id().isBlank())
                .orElseThrow(() -> new GoogleUnavailableException(
                        "Google Calendar did not return a primary account email", null));
        var email = primary.id().trim().toLowerCase(Locale.ROOT);
        var name = primary.summary() == null || primary.summary().isBlank() ? email : primary.summary();
        return new ConnectedGoogleAccount(email, name);
    }

    private GoogleTokenGrant token(String operation, MultiValueMap<String, String> form, String reconnectMessage) {
        if (!properties.configured()) {
            throw new GoogleUnavailableException("Google OAuth client ID and secret are not configured", null);
        }
        form.add(CLIENT_ID, properties.clientId());
        form.add(CLIENT_SECRET, properties.clientSecret());
        var response = post(operation, form);
        var body = response.getBody();
        if (response.getStatusCode().is2xxSuccessful() && body != null && body.accessToken() != null) {
            return toGrant(body);
        }
        var error = body == null || body.error() == null ? UNKNOWN_ERROR : body.error();
        if (INVALID_GRANT.equals(error)) {
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, reconnectMessage);
        }
        if (CLIENT_MISCONFIGURED.contains(error)) {
            throw new GoogleUnavailableException("Google refused the OAuth client (" + error + "); check "
                    + "GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET", null);
        }
        throw new GoogleUnavailableException("Google " + operation + " failed: status="
                + response.getStatusCode().value() + " error=" + error, null);
    }

    /** Error replies are read like successes, so Google's error code can be mapped. */
    private ResponseEntity<GoogleTokenResponse> post(String operation, MultiValueMap<String, String> form) {
        try {
            return oauth.post().uri(properties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                    })
                    .toEntity(GoogleTokenResponse.class);
        } catch (RestClientException exception) {
            // The cause is dropped: a parse failure could quote the reply, which may hold a token.
            throw new GoogleUnavailableException("Google " + operation + " failed: "
                    + exception.getClass().getSimpleName(), null);
        }
    }

    private GoogleTokenGrant toGrant(GoogleTokenResponse body) {
        return GoogleTokenGrant.builder()
                .accessToken(body.accessToken())
                .expiresAt(body.expiresIn() == null ? null : clock.instant().plus(Duration.ofSeconds(body.expiresIn())))
                .refreshToken(body.refreshToken() == null || body.refreshToken().isBlank() ? null : body.refreshToken())
                .scope(body.scope())
                .build();
    }

    /** The first page of the account's calendars, as the Supabase function read it. */
    private List<GoogleCalendarListEntry> calendars(String accessToken) {
        try {
            var response = calendar.get()
                    .uri(uri -> uri.path(CALENDAR_LIST_PATH).queryParam(MIN_ACCESS_ROLE, READER).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleCalendarListResponse.class);
            return response == null || response.items() == null ? List.of() : response.items();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, ACCESS_EXPIRED);
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new GoogleReconnectRequiredException(AuthStatus.REVOKED, ACCESS_REVOKED);
        } catch (RestClientException exception) {
            throw new GoogleUnavailableException("Google calendar list failed: "
                    + exception.getClass().getSimpleName(), exception);
        }
    }

    /** The primary calendar, else one the account owns, else the first. */
    private static Optional<GoogleCalendarListEntry> primaryCalendar(List<GoogleCalendarListEntry> entries) {
        return entries.stream().filter(entry -> Boolean.TRUE.equals(entry.primary())).findFirst()
                .or(() -> entries.stream().filter(entry -> OWNER.equals(entry.accessRole())).findFirst())
                .or(() -> entries.stream().findFirst());
    }
}
