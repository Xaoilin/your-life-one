package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.config.GoogleOAuthProperties;
import com.yourlifeone.calendar.domain.AuthStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuthRestApiTest {
    private static final String TOKEN_URL = "https://oauth.test/token";
    private static final String REVOKE_URL = "https://oauth.test/revoke";
    private static final String CALENDAR = "https://google.test/calendar/v3";
    private static final String CALENDAR_LIST = CALENDAR + "/users/me/calendarList?minAccessRole=reader";
    private static final Instant NOW = Instant.parse("2026-09-27T10:00:00Z");
    private static final GoogleOAuthProperties PROPERTIES =
            new GoogleOAuthProperties("client-id", "client-secret", TOKEN_URL, REVOKE_URL);

    private MockRestServiceServer oauthServer;
    private MockRestServiceServer calendarServer;
    private RestClient oauth;
    private RestClient calendar;
    private GoogleOAuthRestApi api;

    @BeforeEach
    void setUp() {
        var oauthBuilder = RestClient.builder();
        oauthServer = MockRestServiceServer.bindTo(oauthBuilder).build();
        var calendarBuilder = RestClient.builder().baseUrl(CALENDAR);
        calendarServer = MockRestServiceServer.bindTo(calendarBuilder).build();
        oauth = oauthBuilder.build();
        calendar = calendarBuilder.build();
        api = new GoogleOAuthRestApi(oauth, calendar, PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void tokenEndpointReplies(HttpStatus status, String body) {
        oauthServer.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
    }

    @Test
    void exchangesAnAuthorizationCodeWithTheClientCredentials() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", "4/0Ab");
        form.add("redirect_uri", "https://your-life-one.example");
        form.add("client_id", "client-id");
        form.add("client_secret", "client-secret");
        oauthServer.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST)).andExpect(content().formData(form))
                .andRespond(withSuccess("""
                        {"access_token":"access","expires_in":3599,"refresh_token":"refresh",
                         "scope":"https://www.googleapis.com/auth/calendar","token_type":"Bearer"}""",
                        MediaType.APPLICATION_JSON));

        var grant = api.exchangeCode("4/0Ab", "https://your-life-one.example");

        assertEquals("access", grant.accessToken());
        assertEquals("refresh", grant.refreshToken());
        assertEquals(NOW.plusSeconds(3599), grant.expiresAt());
        assertFalse(grant.toString().contains("refresh\""));
        oauthServer.verify();
    }

    @Test
    void refreshesWithTheStoredRefreshToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", "refresh");
        form.add("client_id", "client-id");
        form.add("client_secret", "client-secret");
        oauthServer.expect(requestTo(TOKEN_URL)).andExpect(content().formData(form))
                .andRespond(withSuccess("{\"access_token\":\"fresh\",\"expires_in\":3599}", MediaType.APPLICATION_JSON));

        var grant = api.refresh("refresh");

        assertEquals("fresh", grant.accessToken());
        assertNull(grant.refreshToken());
    }

    @Test
    void anInvalidGrantMeansTheUserMustReconnect() {
        tokenEndpointReplies(HttpStatus.BAD_REQUEST,
                "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}");

        var refused = assertThrows(GoogleReconnectRequiredException.class, () -> api.refresh("refresh"));

        assertEquals(AuthStatus.NEEDS_RECONNECT, refused.authStatus());
    }

    @Test
    void aMisconfiguredClientOrAGoogleOutageIsUnavailableNotAReconnect() {
        tokenEndpointReplies(HttpStatus.UNAUTHORIZED, "{\"error\":\"invalid_client\",\"error_description\":\"Unauthorized\"}");
        assertThrows(GoogleUnavailableException.class, () -> api.refresh("refresh"));

        oauthServer.reset();
        oauthServer.expect(requestTo(TOKEN_URL)).andRespond(withServerError());
        assertThrows(GoogleUnavailableException.class, () -> api.refresh("refresh"));
    }

    @Test
    void refusesToCallGoogleWithoutClientCredentials() {
        var unconfigured = new GoogleOAuthRestApi(oauth, calendar,
                new GoogleOAuthProperties("", "", TOKEN_URL, REVOKE_URL), Clock.systemUTC());

        assertThrows(GoogleUnavailableException.class, () -> unconfigured.refresh("refresh"));
        oauthServer.verify();
    }

    @Test
    void identifiesTheAccountFromItsPrimaryCalendar() {
        calendarServer.expect(requestTo(CALENDAR_LIST)).andExpect(header("Authorization", "Bearer access"))
                .andRespond(withSuccess("""
                        {"items":[{"id":"holidays@group","summary":"Holidays","accessRole":"reader"},
                                  {"id":"YourLife@Example.com","summary":"YourLife","primary":true,"accessRole":"owner"}]}""",
                        MediaType.APPLICATION_JSON));

        var account = api.account("access");

        assertEquals("yourlife@example.com", account.email());
        assertEquals("YourLife", account.name());
    }

    @Test
    void aForbiddenCalendarListMeansAccessWasRevoked() {
        calendarServer.expect(requestTo(CALENDAR_LIST)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertEquals(AuthStatus.REVOKED,
                assertThrows(GoogleReconnectRequiredException.class, () -> api.account("access")).authStatus());

        calendarServer.reset();
        calendarServer.expect(requestTo(CALENDAR_LIST)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertEquals(AuthStatus.NEEDS_RECONNECT,
                assertThrows(GoogleReconnectRequiredException.class, () -> api.account("access")).authStatus());
    }

    @Test
    void revokingIsBestEffort() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("token", "refresh");
        oauthServer.expect(requestTo(REVOKE_URL)).andExpect(content().formData(form))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertDoesNotThrow(() -> api.revoke("refresh"));
        oauthServer.verify();
    }
}
