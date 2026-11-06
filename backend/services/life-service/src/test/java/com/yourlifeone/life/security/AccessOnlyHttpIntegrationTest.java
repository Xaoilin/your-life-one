package com.yourlifeone.life.security;

import com.yourlifeone.commonservice.testing.AccessTokens;
import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresQueryCounts;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.infrastructure.persistence.LifeSchemas;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Real HTTP, production ES256 validation, real approval rows and server-side PostgreSQL query counts. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
class AccessOnlyHttpIntegrationTest {
    private static final AccessTokens TOKENS = tokens();
    private static final String BASE = "/api/life/v1";
    private static final ContractFixtures CONTRACTS = new ContractFixtures("contracts/life-service");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @MockitoSpyBean
    private LifeSchemas schemas;

    private JdbcClient jdbc;
    private PostgresQueryCounts counts;

    @DynamicPropertySource
    static void issuer(DynamicPropertyRegistry properties) {
        properties.add("app.auth.supabase-url", TOKENS::baseUrl);
    }

    @BeforeEach
    void setUp() {
        // Finish independent startup migrations before measuring one HTTP request.
        doCallRealMethod().when(schemas).ensureMigrated();
        schemas.ensureMigrated();
        clearInvocations(schemas);
        jdbc = JdbcClient.create(dataSource);
        counts = new PostgresQueryCounts(dataSource);
        for (var table : new String[] {"yourlifeone_inventory_oauth_clients", "yourlifeone_employment_oauth_clients"}) {
            jdbc.sql("CREATE TABLE IF NOT EXISTS public." + table
                    + " (user_id uuid, client_id text, revoked_at timestamptz)").update();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "inventory, yourlifeone_inventory_oauth_clients, /inventory/search, inventory-access",
        "jobs, yourlifeone_employment_oauth_clients, /jobs/applications, jobs-access"
    })
    void accessPreservesApprovalOwnershipAndImmediateRevocationWithoutBusinessReads(
            String domain, String table, String legacy, String fixture) throws Exception {
        var user = UUID.randomUUID();
        var client = "access-" + UUID.randomUUID();
        var path = BASE + "/" + domain + "/access";
        approve(table, user, client);
        var token = TOKENS.token(user.toString(), client);

        // Replay a cold/unavailable business-schema boundary without bypassing JWT or approval.
        doThrow(new IllegalStateException("Access must not initialize business schemas"))
                .when(schemas).ensureMigrated();
        counts.reset();
        var response = get(path, token);
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        CONTRACTS.check(fixture, "GET", path, response.statusCode(), response.body());
        assertApprovalOnly(table);

        // Neither another account, another OAuth client nor another domain inherits this approval.
        assertStatusAndQueries(path, TOKENS.token(UUID.randomUUID().toString(), client), 403, 2);
        assertStatusAndQueries(path, TOKENS.token(user.toString(), "unapproved"), 403, 2);
        var otherDomain = domain.equals("inventory") ? "jobs" : "inventory";
        assertStatusAndQueries(BASE + "/" + otherDomain + "/access", token, 403, 2);

        jdbc.sql("UPDATE public." + table + " SET revoked_at = now() WHERE user_id = ? AND client_id = ?")
                .params(user, client).update();
        assertStatusAndQueries(path, token, 403, 2);
        var denied = get(path, token);
        assertThat(denied.body()).contains("agent_not_approved");
        CONTRACTS.check("agent-not-approved", "GET", path, denied.statusCode(), denied.body());

        // App tokens keep their existing approval-free semantics, with a validated account subject.
        assertStatusAndQueries(path, TOKENS.token(user.toString(), null), 204, 0);
        assertStatusAndQueries(path, TOKENS.token("not-an-account", null), 401, 0);
        assertStatusAndQueries(path, TOKENS.token("not-an-account", client), 401, 0);
        assertStatusAndQueries(path, null, 401, 0);
        assertStatusAndQueries(path, "invalid-token", 401, 0);
        assertStatusAndQueries(path, TOKENS.token(user.toString(), client, "authenticated",
                TOKENS.baseUrl() + "/auth/v1", Instant.now().minusSeconds(300)), 401, 0);
        assertStatusAndQueries(path, TOKENS.token(user.toString(), client, "anon",
                TOKENS.baseUrl() + "/auth/v1", Instant.now().plusSeconds(300)), 401, 0);
        assertStatusAndQueries(path, TOKENS.token(user.toString(), client, "authenticated",
                "https://other-issuer.invalid/auth/v1", Instant.now().plusSeconds(300)), 401, 0);
        try (var wrongSigner = new AccessTokens()) {
            assertStatusAndQueries(path, wrongSigner.token(user.toString(), client), 401, 0);
        }
        verify(schemas, never()).ensureMigrated();
        System.out.println("KAN-326 " + path + " approved=204 approval_selects=2 business_selects=0"
                + " revoked=403 other_account=403 other_client=403 other_domain=403 invalid_tokens=401");
    }

    @ParameterizedTest
    @CsvSource({
        "inventory, yourlifeone_inventory_oauth_clients, /inventory/search, inventory-access",
        "jobs, yourlifeone_employment_oauth_clients, /jobs/applications, jobs-access"
    })
    void originalBusinessRoutesRemainCompatibleAndProveTheQueryCounter(
            String domain, String table, String legacy, String fixture) throws Exception {
        var user = UUID.randomUUID();
        var client = "legacy-" + UUID.randomUUID();
        approve(table, user, client);
        counts.reset();
        assertThat(get(BASE + legacy, TOKENS.token(user.toString(), client)).statusCode()).isEqualTo(200);
        assertThat(counts.selects().keySet()).anyMatch(query -> query.matches("(?is).*from (inventory|jobs)\\..*"));
        System.out.println("KAN-326 " + BASE + legacy + " legacy=200 business_selects_detected=true");
    }

    private void assertApprovalOnly(String table) {
        var queries = counts.selects();
        assertThat(queries).hasSize(2);
        assertThat(queries.values()).allMatch(calls -> calls == 1);
        assertThat(queries.keySet()).allMatch(query -> query.startsWith("SELECT to_regclass(")
                || query.startsWith("SELECT EXISTS (SELECT") && query.contains("FROM public." + table + " "));
    }

    private void assertStatusAndQueries(String path, String token, int status, long selects) throws Exception {
        counts.reset();
        assertThat(get(path, token).statusCode()).as(path).isEqualTo(status);
        assertThat(counts.selects().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(selects);
    }

    private void approve(String table, UUID user, String client) {
        jdbc.sql("INSERT INTO public." + table + " (user_id, client_id, revoked_at) VALUES (?, ?, NULL)")
                .params(user, client).update();
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static AccessTokens tokens() {
        try {
            return new AccessTokens();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @AfterAll
    static void closeIssuer() {
        TOKENS.close();
    }
}
