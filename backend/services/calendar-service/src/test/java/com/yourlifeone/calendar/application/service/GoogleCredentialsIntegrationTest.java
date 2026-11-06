package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.model.ConnectedGoogleAccount;
import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleTokenGrant;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.infrastructure.crypto.EncryptedToken;
import com.yourlifeone.calendar.infrastructure.crypto.GoogleCredentialCipher;
import com.yourlifeone.calendar.infrastructure.persistence.importing.SupabaseGoogleCredentialImport;
import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google credentials held by this service: stored encrypted, refreshed on demand, marked for
 * reconnection when Google refuses them, revoked with their account, and imported once from Supabase.
 * Google's OAuth and Calendar APIs are mocked; PostgreSQL and RabbitMQ are real.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class GoogleCredentialsIntegrationTest {
    private static final String EMAIL = "yourlife@example.com";
    private static final String REFRESH_TOKEN = "1//refresh-token-for-yourlife";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CONNECT = "/api/calendar/v1/accounts/google";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private GoogleCredentialCipher cipher;

    @Autowired
    private GoogleCredentials credentials;

    @Autowired
    private GoogleCredentialStore store;

    @Autowired
    private SupabaseGoogleCredentialImport supabaseImport;

    @Autowired
    private BusinessGaugePublisher gauges;

    @Autowired
    private MeterRegistry meters;

    @MockitoBean
    private GoogleOAuthApi oauth;

    @MockitoBean
    private GoogleCalendarApi google;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void googleGrantsACredentialForOneAccount() {
        when(oauth.exchangeCode(eq("code-1"), anyString())).thenReturn(grant("access-1", REFRESH_TOKEN, 3600));
        when(oauth.account(anyString())).thenReturn(new ConnectedGoogleAccount(EMAIL, "YourLife"));
        when(oauth.refresh(REFRESH_TOKEN)).thenReturn(grant("access-2", null, 3600));
        when(google.calendars(anyString())).thenReturn(List.of(GoogleCalendar.builder().id(EMAIL).summary("YourLife")
                .primary(true).accessRole("owner").build()));
        when(google.events(anyString(), eq(EMAIL), any(), any())).thenReturn(List.of());
    }

    private static GoogleTokenGrant grant(String accessToken, String refreshToken, long lifetimeSeconds) {
        return GoogleTokenGrant.builder().accessToken(accessToken).refreshToken(refreshToken)
                .expiresAt(Instant.now().plusSeconds(lifetimeSeconds)).scope("calendar").build();
    }

    private ResultActions perform(UUID userId, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(userId.toString()))));
    }

    private ResultActions connect(UUID userId, String body) throws Exception {
        return perform(userId, post(CONNECT).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions connect() throws Exception {
        return connect(user, "{\"code\":\"code-1\",\"redirectUri\":\"https://your-life-one.example\"}");
    }

    private EncryptedToken stored(UUID userId, String email) {
        return jdbc.sql("""
                        SELECT refresh_token_ciphertext, refresh_token_nonce, key_version
                        FROM calendar.google_credential WHERE user_id = ? AND email = ?""")
                .params(userId, email)
                .query((rs, row) -> new EncryptedToken(rs.getBytes(1), rs.getBytes(2), rs.getInt(3)))
                .single();
    }

    private long credentialCount(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM calendar.google_credential WHERE user_id = ?").param(userId)
                .query(Long.class).single();
    }

    private OffsetDateTime column(String column, UUID userId, String email) {
        return jdbc.sql("SELECT " + column + " FROM calendar.google_credential WHERE user_id = ? AND email = ?")
                .params(userId, email).query(OffsetDateTime.class).optional().orElse(null);
    }

    @Test
    void connectingStoresTheRefreshTokenEncryptedAndNeverAnAccessToken() throws Exception {
        connect().andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));

        var encrypted = stored(user, EMAIL);
        assertFalse(new String(encrypted.ciphertext(), StandardCharsets.ISO_8859_1).contains(REFRESH_TOKEN));
        assertEquals(GoogleCredentialCipher.NONCE_BYTES, encrypted.nonce().length);
        assertEquals(REFRESH_TOKEN, cipher.decrypt(encrypted, user, EMAIL));
        var columns = jdbc.sql("""
                        SELECT string_agg(column_name, ',') FROM information_schema.columns
                        WHERE table_schema = 'calendar' AND table_name = 'google_credential'""")
                .query(String.class).single();
        assertFalse(columns.contains("access_token"));
    }

    @Test
    void reconnectingWithoutANewRefreshTokenKeepsTheStoredOneAndAFirstConnectionNeedsOne() throws Exception {
        connect().andExpect(status().isOk());
        when(oauth.exchangeCode(eq("code-2"), anyString())).thenReturn(grant("access-3", null, 3600));

        connect(user, "{\"code\":\"code-2\",\"redirectUri\":\"https://your-life-one.example\"}").andExpect(status().isOk());
        assertEquals(REFRESH_TOKEN, cipher.decrypt(stored(user, EMAIL), user, EMAIL));

        var newcomer = UUID.randomUUID();
        connect(newcomer, "{\"code\":\"code-2\",\"redirectUri\":\"https://your-life-one.example\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("google_reconnect_required"));
        assertEquals(0, credentialCount(newcomer));
    }

    @Test
    void anAccessTokenIsRefreshedWhenItIsAboutToExpire() throws Exception {
        when(oauth.refresh(REFRESH_TOKEN)).thenReturn(grant("short-lived", null, 60));
        connect().andExpect(status().isOk());
        perform(user, post("/api/calendar/v1/sync")).andExpect(jsonPath("$.accounts[0].status").value("synced"));
        verify(oauth, times(2)).refresh(REFRESH_TOKEN);

        when(oauth.refresh(REFRESH_TOKEN)).thenReturn(grant("long-lived", null, 3600));
        perform(user, post("/api/calendar/v1/sync")).andExpect(status().isOk());
        perform(user, post("/api/calendar/v1/sync")).andExpect(status().isOk());

        verify(oauth, times(3)).refresh(REFRESH_TOKEN);
        verify(google, times(2)).calendars("long-lived");
        assertNotNull(column("last_refresh_at", user, EMAIL));
    }

    @Test
    void aRefusedRefreshTokenIsRecordedAndSyncAsksForReconnection() throws Exception {
        when(oauth.refresh(REFRESH_TOKEN)).thenReturn(grant("short-lived", null, 60));
        connect().andExpect(status().isOk());
        when(oauth.refresh(REFRESH_TOKEN)).thenThrow(new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT,
                "Google refresh token expired or was revoked. Reconnect this account."));

        perform(user, post("/api/calendar/v1/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].status").value("needs_reconnect"));

        assertNotNull(column("last_failure_at", user, EMAIL));
        assertNull(column("revoked_at", user, EMAIL));
        gauges.refresh();
        long needingReconnect = jdbc.sql("""
                SELECT count(*) FROM calendar.google_credential
                WHERE revoked_at IS NULL AND last_failure_at IS NOT NULL""").query(Long.class).single();
        assertEquals((double) needingReconnect,
                meters.get("yourlife.calendar.google.credentials").tag("status", "needs_reconnect").gauge().value());
    }

    @Test
    void removingAGoogleAccountRevokesAndDeletesItsCredential() throws Exception {
        var accountId = JSON.readTree(connect().andReturn().getResponse().getContentAsString()).path("id").asString();

        perform(user, delete("/api/calendar/v1/accounts/" + accountId)).andExpect(status().isNoContent());

        verify(oauth).revoke(REFRESH_TOKEN);
        assertEquals(0, credentialCount(user));
        assertDoesNotThrow(() -> credentials.revoke(UUID.randomUUID(), "nobody@example.com"));
    }

    @Test
    void anotherGoogleAccountIsRefusedWhenReconnectingAndItsGrantRevoked() throws Exception {
        when(oauth.account("access-1")).thenReturn(new ConnectedGoogleAccount("other@example.com", "Other"));

        connect(user, """
                {"code":"code-1","redirectUri":"https://your-life-one.example","expectedEmail":"YourLife@Example.com"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("google_reconnect_required"));

        verify(oauth).revoke("access-1");
        verify(oauth, never()).refresh(anyString());
        assertEquals(0, credentialCount(user));
    }

    @Test
    void importsSupabaseCredentialsOnceEncryptingEachToken() {
        supabaseImport.ensureImported();
        var other = UUID.randomUUID();
        store.save(user, "kept@example.com", grant("access", "current-token", 3600));
        jdbc.sql("""
                CREATE TABLE public.google_calendar_credentials (
                    user_id uuid NOT NULL, google_email text NOT NULL, refresh_token text NOT NULL,
                    access_token text, access_token_expires_at timestamptz, scope text,
                    credential_origin text NOT NULL DEFAULT 'oauth_code', last_refresh_at timestamptz,
                    last_refresh_failure_reason text, last_refresh_failure_at timestamptz, revoked_at timestamptz,
                    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
                    PRIMARY KEY (user_id, google_email))""").update();
        try {
            legacy(user, "YourLife@Example.com", "legacy-token-1", null, false);
            legacy(user, "gone@example.com", "legacy-token-2", null, true);
            legacy(user, "kept@example.com", "legacy-token-3", null, false);
            legacy(other, "work@example.com", "legacy-token-4", "invalid_grant", false);

            assertEquals(2, supabaseImport.importCredentials());

            assertEquals("legacy-token-1", cipher.decrypt(stored(user, EMAIL), user, EMAIL));
            assertEquals("legacy-token-4", cipher.decrypt(stored(other, "work@example.com"), other, "work@example.com"));
            assertEquals("current-token", cipher.decrypt(stored(user, "kept@example.com"), user, "kept@example.com"));
            assertNull(column("created_at", user, "gone@example.com"));
            assertNotNull(column("last_failure_at", other, "work@example.com"));
            assertEquals(0, supabaseImport.importCredentials());
        } finally {
            jdbc.sql("DROP TABLE public.google_calendar_credentials").update();
        }
    }

    private void legacy(UUID userId, String email, String refreshToken, String failure, boolean revoked) {
        jdbc.sql("""
                        INSERT INTO public.google_calendar_credentials (user_id, google_email, refresh_token,
                            access_token, last_refresh_failure_reason, last_refresh_failure_at, revoked_at)
                        VALUES (?, ?, ?, 'plain-access-token', CAST(? AS text),
                                CASE WHEN CAST(? AS text) IS NULL THEN NULL ELSE now() END,
                                CASE WHEN ? THEN now() END)""")
                .params(userId, email, refreshToken, failure, failure, revoked)
                .update();
    }
}
