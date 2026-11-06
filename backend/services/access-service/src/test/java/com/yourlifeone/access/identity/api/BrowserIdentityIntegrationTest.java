package com.yourlifeone.access.identity.api;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.yourlifeone.access.testing.AccessDatabaseTestConfiguration;
import com.yourlifeone.access.testing.SyntheticIdentityProvider;
import com.yourlifeone.access.testing.SyntheticProductBackend;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP, signed OIDC exchange, JDBC sessions, migrations, JWT verification and CSRF. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.hikari.data-source-properties.sslmode=disable",
    "server.servlet.session.cookie.secure=false"
})
@Import(AccessDatabaseTestConfiguration.class)
class BrowserIdentityIntegrationTest {
    private static final String ISSUER = "https://api.yourlife.example/api/access";
    private static final String BROWSER = "https://yourlife.example/yourlifeone/";
    private static final UUID USER = UUID.randomUUID();
    private static final RSAKey SIGNING_KEY;
    private static final SyntheticIdentityProvider PROVIDER;
    private static final SyntheticProductBackend BACKEND;

    static {
        try {
            SIGNING_KEY = new RSAKeyGenerator(2048).keyID("product-test").generate();
            PROVIDER = new SyntheticIdentityProvider();
            BACKEND = new SyntheticProductBackend();
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate database;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JwtEncoder encoder;
    private HttpClient browser;

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry properties) {
        properties.add("app.access.public-url", () -> ISSUER);
        properties.add("app.access.browser-url", () -> BROWSER);
        properties.add("app.access.allowed-origins", () -> "https://yourlife.example");
        properties.add("app.access.signing-keys", () -> new JWKSet(SIGNING_KEY).toString(false));
        properties.add("app.access.signing-key-id", () -> SIGNING_KEY.getKeyID());
        properties.add("app.mcp.backends.life", BACKEND::url);
        properties.add("app.mcp.backends.finance", BACKEND::url);
        properties.add("app.mcp.backends.knowledge", BACKEND::url);
        var client = "spring.security.oauth2.client.registration.google.";
        properties.add(client + "client-id", () -> "synthetic-client");
        properties.add(client + "client-secret", () -> "synthetic-secret");
        properties.add(client + "redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");
        var provider = "spring.security.oauth2.client.provider.google.";
        properties.add(provider + "authorization-uri", () -> PROVIDER.url() + "/authorize");
        properties.add(provider + "token-uri", () -> PROVIDER.url() + "/token");
        properties.add(provider + "user-info-uri", () -> PROVIDER.url() + "/userinfo");
        properties.add(provider + "jwk-set-uri", () -> PROVIDER.url() + "/jwks");
        properties.add(provider + "issuer-uri", PROVIDER::url);
    }

    @BeforeEach
    void freshBrowserAndExistingIdentity() {
        browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        PROVIDER.subject("existing-google-subject");
        BACKEND.reset();
        database.update("delete from identity.account_identity");
        database.update("insert into identity.account_identity (id,user_id,provider,subject,email,enabled) "
                + "values (?,?, 'google','existing-google-subject','synthetic@example.test',true)", UUID.randomUUID(), USER);
    }

    @AfterAll
    static void stopProvider() {
        PROVIDER.close();
        BACKEND.close();
    }

    @Test
    void realSignInKeepsTheAccountAndSessionLogoutRequiresCsrf() throws Exception {
        assertThat(body(get("/session")).get("user").isNull()).isTrue();
        assertThat(signIn()).isEqualTo(BROWSER);
        var cookies = ((CookieManager) browser.cookieHandler().orElseThrow()).getCookieStore().getCookies();
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie.getName()).endsWith("_SESSION");
            assertThat(cookie.getMaxAge()).isEqualTo(604800);
            assertThat(cookie.isHttpOnly()).isTrue();
        });

        var response = get("/session");
        var session = body(response);
        assertThat(session.at("/user/id").asText()).isEqualTo(USER.toString());
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        var decoder = NimbusJwtDecoder.withPublicKey(SIGNING_KEY.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        var token = decoder.decode(session.get("accessToken").asText());
        assertThat(token.getSubject()).isEqualTo(USER.toString());
        assertThat(token.getClaims()).doesNotContainKeys("client_id", "refresh_token");
        assertThat(database.queryForObject("select count(*) from identity.spring_session", Integer.class)).isPositive();

        var noCsrf = browser.send(HttpRequest.newBuilder(url("/signout")).POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        var csrf = body(get("/csrf"));
        var logout = browser.send(HttpRequest.newBuilder(url("/signout"))
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(body(get("/session")).get("user").isNull()).isTrue();
    }

    @Test
    void anotherGoogleSubjectCannotClaimTheSameEmailOrCreateAnAccount() throws Exception {
        PROVIDER.subject("unknown-google-subject");

        assertThat(signIn()).isEqualTo(BROWSER + "?auth=failed");
        assertThat(body(get("/session")).get("user").isNull()).isTrue();
        assertThat(database.queryForObject("select count(*) from identity.account_identity", Integer.class)).isEqualTo(1);
    }

    @Test
    void publicKeysArePublicOnlyAndBrowserCredentialsAreNotSharedWithAnotherOrigin() throws Exception {
        var keys = JWKSet.parse(get("/.well-known/jwks.json").body());
        assertThat(keys.getKeyByKeyId("product-test").isPrivate()).isFalse();
        var rejected = browser.send(HttpRequest.newBuilder(url("/session"))
                .header("Origin", "https://attacker.example").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(rejected.statusCode()).isEqualTo(403);
        var trusted = browser.send(HttpRequest.newBuilder(url("/session"))
                .header("Origin", "https://yourlife.example").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(trusted.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://yourlife.example");
        assertThat(trusted.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
    }

    @Test
    void browserAgentClientsCanReadPublicDiscoveryAndTokenPreflightWithoutBrowserCookieAccess() throws Exception {
        var metadata = browser.send(HttpRequest.newBuilder(url("/mcp-metadata/inventory"))
                .header("Origin", "https://agent.example").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(metadata.statusCode()).isEqualTo(200);
        assertThat(metadata.headers().firstValue("Access-Control-Allow-Origin")).hasValue("*");
        assertThat(metadata.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
        var preflight = browser.send(HttpRequest.newBuilder(url("/oauth2/token"))
                .header("Origin", "https://agent.example").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(preflight.statusCode()).isEqualTo(200);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue("*");
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
    }

    @Test
    void publicAgentUsesPkceAndRotatingRefreshTokensWhoseStoredHashesCannotBeReplayed() throws Exception {
        var registration = postJson("/oauth2/register", """
                {"client_name":"Synthetic agent","redirect_uris":["http://127.0.0.1:32145/callback"],
                 "grant_types":["authorization_code","refresh_token"],"response_types":["code"],
                 "token_endpoint_auth_method":"none","scope":"mcp"}
                """);
        assertThat(registration.statusCode()).isEqualTo(201);
        var clientId = body(registration).get("client_id").asText();
        assertThat(signIn()).isEqualTo(BROWSER);
        var verifier = "synthetic-pkce-verifier-which-is-longer-than-forty-three-characters";
        var challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        var redirect = "http://127.0.0.1:32145/callback";
        var authorizeUri = url("/oauth2/authorize?response_type=code&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirect) + "&scope=mcp&state=synthetic-state"
                + "&code_challenge_method=S256&code_challenge=" + challenge);
        var authorize = browser.send(HttpRequest.newBuilder(authorizeUri).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(authorize.statusCode()).isEqualTo(302);
        var consentPage = browser.send(HttpRequest.newBuilder(authorizeUri.resolve(
                authorize.headers().firstValue("Location").orElseThrow())).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(consentPage.statusCode()).isEqualTo(303);
        var consentUri = URI.create(consentPage.headers().firstValue("Location").orElseThrow());
        assertThat(consentUri.toString()).startsWith(BROWSER + "oauth/consent?");
        var consentState = parameters(consentUri.getRawQuery()).get("authorization_id");
        var details = body(get("/consent/" + encode(consentState)));
        assertThat(details.at("/client/id").asText()).isEqualTo(clientId);
        assertThat(details.at("/user/id").asText()).isEqualTo(USER.toString());
        assertThat(details.path("redirect_uri").asText()).isEqualTo(redirect);
        var csrf = body(get("/csrf"));
        var approved = postForm("/oauth2/authorize", "client_id=" + encode(clientId)
                + "&state=" + encode(consentState) + "&scope=mcp", csrf);
        assertThat(approved.statusCode()).isBetween(300, 399);
        var callback = URI.create(approved.headers().firstValue("Location").orElseThrow());
        assertThat(callback.toString()).startsWith(redirect);
        var code = parameters(callback.getRawQuery()).get("code");
        assertThat(code).isNotBlank();
        var grant = "grant_type=authorization_code&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirect) + "&code_verifier=" + encode(verifier) + "&code=" + encode(code);
        var issued = postForm("/oauth2/token", grant, null);
        assertThat(issued.statusCode()).isEqualTo(200);
        var tokens = body(issued);
        var decoder = NimbusJwtDecoder.withPublicKey(SIGNING_KEY.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        var access = decoder.decode(tokens.get("access_token").asText());
        assertThat(access.getSubject()).isEqualTo(USER.toString());
        assertThat(access.getClaimAsString("client_id")).isEqualTo(clientId);
        assertThat(access.getAudience()).contains("authenticated", ISSUER + "/mcp", ISSUER + "/mcp/finance",
                ISSUER + "/mcp/equity", ISSUER + "/mcp/inventory", ISSUER + "/mcp/employment");

        var refresh = tokens.get("refresh_token").asText();
        var storedHash = database.queryForObject("select refresh_token_value from identity.oauth2_authorization "
                + "where principal_name=? and refresh_token_value is not null", String.class, USER.toString());
        assertThat(storedHash).startsWith("{sha256}").isNotEqualTo(refresh);
        var refreshGrant = "grant_type=refresh_token&client_id=" + encode(clientId) + "&refresh_token=";
        assertThat(postForm("/oauth2/token", refreshGrant + encode(storedHash), null).statusCode()).isEqualTo(400);
        var renewed = postForm("/oauth2/token", refreshGrant + encode(refresh), null);
        assertThat(renewed.statusCode()).as("Refresh error: %s", body(renewed).path("error").asText()).isEqualTo(200);
        assertThat(body(renewed).get("refresh_token").asText()).isNotEqualTo(refresh);
        assertThat(postForm("/oauth2/token", refreshGrant + encode(refresh), null).statusCode()).isEqualTo(400);
        // Replaying a code can revoke the entire grant, so check replay after the refresh assertions.
        assertThat(postForm("/oauth2/token", grant, null).statusCode()).isEqualTo(400);
    }

    @Test
    void oauthDiscoveryAndUnauthorizedMcpChallengeUseOnlyTheNativeIssuer() throws Exception {
        var authorization = get("/.well-known/oauth-authorization-server");
        assertThat(authorization.statusCode()).isEqualTo(200);
        var metadata = body(authorization);
        assertThat(metadata.path("issuer").asText()).isEqualTo(ISSUER);
        assertThat(metadata.path("authorization_endpoint").asText()).isEqualTo(ISSUER + "/oauth2/authorize");
        assertThat(metadata.path("token_endpoint").asText()).isEqualTo(ISSUER + "/oauth2/token");
        assertThat(metadata.path("registration_endpoint").asText()).isEqualTo(ISSUER + "/oauth2/register");
        assertThat(metadata.path("code_challenge_methods_supported").toString()).contains("S256");
        var resource = get("/.well-known/oauth-protected-resource");
        assertThat(resource.statusCode()).isEqualTo(200);
        assertThat(body(resource).path("resource").asText()).isEqualTo(ISSUER + "/mcp");
        assertThat(body(resource).path("authorization_servers").get(0).asText()).isEqualTo(ISSUER);
        var denied = browser.send(HttpRequest.newBuilder(url("/mcp")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(401);
        assertThat(denied.headers().firstValue("WWW-Authenticate").orElseThrow())
                .contains("resource_metadata=\"" + ISSUER + "/.well-known/oauth-protected-resource\"");
    }

    @Test
    void dynamicRegistrationRefusesRemotePlainHttpRedirectsAndClientCredentialsGrants() throws Exception {
        assertThat(postJson("/oauth2/register", """
                {"redirect_uris":["http://attacker.example/callback"],"token_endpoint_auth_method":"none"}
                """).statusCode()).isEqualTo(400);
        assertThat(postJson("/oauth2/register", """
                {"redirect_uris":["https://agent.example/callback"],"grant_types":["client_credentials"]}
                """).statusCode()).isEqualTo(400);
    }

    @Test
    void nativeMcpDiscoveryPreservesAllTwentyFourLegacyToolContracts() throws Exception {
        var reply = mcp("tools/list", Map.of(), "native-agent");
        var actual = reply.at("/result/tools");
        assertThat(actual.size()).as("MCP discovery response: %s", reply).isEqualTo(24);
        for (String domain : List.of("finance", "equity", "inventory", "employment")) {
            try (var input = new ClassPathResource("mcp/" + domain + "-tools.json").getInputStream()) {
                for (var expected : json.readTree(input)) {
                    JsonNode found = null;
                    for (var tool : actual) {
                        if (tool.path("name").equals(expected.path("name"))) found = tool;
                    }
                    assertThat(found).as("Legacy tool %s remains discoverable", expected.path("name").asText()).isNotNull();
                    for (String field : List.of("name", "title", "description", "annotations")) {
                        assertThat(found.path(field)).isEqualTo(expected.path(field));
                    }
                    // $schema identifies the JSON dialect; the SDK retains the actual input constraints.
                    for (String field : List.of("type", "properties", "required", "additionalProperties")) {
                        assertThat(found.path("inputSchema").path(field)).isEqualTo(expected.path("inputSchema").path(field));
                    }
                }
            }
        }
        assertThat(BACKEND.requests()).isEmpty();
    }

    @Test
    void separateMcpConnectionsExposeOnlyTheirOwnDomainEvenWhenTheClientHasAllApprovals() throws Exception {
        var counts = Map.of("finance", 6, "equity", 5, "inventory", 7, "employment", 6);
        for (var endpoint : counts.entrySet()) {
            var metadata = body(get("/mcp-metadata/" + endpoint.getKey()));
            assertThat(metadata.path("resource").asText()).isEqualTo(ISSUER + "/mcp/" + endpoint.getKey());
            var tools = mcp("tools/list", Map.of(), "native-agent", endpoint.getKey()).at("/result/tools");
            assertThat(tools.size()).isEqualTo(endpoint.getValue());
            for (var tool : tools) assertThat(tool.path("name").asText()).startsWith(endpoint.getKey() + "_");
        }
        var rejected = mcp("tools/call", Map.of("name", "finance_get_review", "arguments", Map.of()),
                "native-agent", "inventory");
        assertThat(rejected.at("/result/isError").asBoolean()).isTrue();
        assertThat(BACKEND.requests()).isEmpty();
    }

    @Test
    void nativeMcpKeepsDomainApprovalAndCallerIdentityAndRechecksAfterRevocation() throws Exception {
        BACKEND.allow(Set.of("finance"));
        assertThat(mcp("tools/list", Map.of(), "native-agent").at("/result/tools").size()).isEqualTo(6);
        assertThat(mcp("tools/list", Map.of(), "another-agent").at("/result/tools").isEmpty()).isTrue();
        var denied = mcp("tools/call", Map.of("name", "employment_list_applications", "arguments", Map.of()), "native-agent");
        assertThat(denied.at("/result/isError").asBoolean()).isTrue();
        assertThat(BACKEND.requests()).isEmpty();

        var read = mcp("tools/call", Map.of("name", "finance_get_review", "arguments", Map.of()), "native-agent");
        assertThat(read.at("/result/structuredContent/result").isNull()).isTrue();
        assertThat(BACKEND.requests()).hasSize(1);
        assertThat(BACKEND.requests().getFirst().userId()).isEqualTo(USER.toString());
        assertThat(BACKEND.requests().getFirst().clientId()).isEqualTo("native-agent");
        BACKEND.allow(Set.of());
        var revoked = mcp("tools/call", Map.of("name", "finance_get_review", "arguments", Map.of()), "native-agent");
        assertThat(revoked.at("/result/isError").asBoolean()).isTrue();
        assertThat(BACKEND.requests()).hasSize(1);
    }

    @Test
    void nativeMcpPreservesMutationGuardsPatchClearsAndIdempotency() throws Exception {
        var blockedBatch = mcp("tools/call", Map.of("name", "inventory_save_items", "arguments",
                Map.of("items", List.of(Map.of("name", "Paper"), Map.of("name", "Pens")))), "native-agent");
        assertThat(blockedBatch.at("/result/isError").asBoolean()).isTrue();
        assertThat(BACKEND.requests()).isEmpty();

        var requestId = UUID.randomUUID().toString();
        var patch = new LinkedHashMap<String, Object>();
        patch.put("url", null);
        patch.put("status", "applied");
        var updated = mcp("tools/call", Map.of("name", "employment_update_application", "arguments",
                Map.of("requestId", requestId, "applicationId", "existing-job", "patch", patch)), "native-agent");
        assertThat(updated.has("error")).isFalse();
        assertThat(updated.at("/result/isError").asBoolean()).isFalse();
        assertThat(BACKEND.requests()).hasSize(1);
        var sent = BACKEND.requests().getFirst();
        assertThat(sent.method()).isEqualTo("PATCH");
        assertThat(sent.path()).isEqualTo("/api/life/v1/jobs/applications/existing-job");
        assertThat(sent.requestId()).isEqualTo(requestId);
        assertThat(sent.body().path("clear").get(0).asText()).isEqualTo("url");
        assertThat(sent.body().has("url")).isFalse();
        assertThat(sent.body().path("status").asText()).isEqualTo("applied");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loanProfileToolsPreserveTheSavedScenarioAndItsVersionAndRequireDeletionConfirmation() throws Exception {
        JsonNode fixture;
        try (var input = new ClassPathResource("review-loan-profiles.json").getInputStream()) {
            fixture = json.readTree(input).path("body");
        }
        BACKEND.review(json.writeValueAsString(fixture));
        var profile = fixture.at("/review/loanProfiles/0");
        var version = fixture.at("/review/updatedAt").asText();
        var listed = mcp("tools/call", Map.of("name", "finance_list_loan_profiles", "arguments", Map.of()), "native-agent");
        assertThat(listed.at("/result/structuredContent/result/profiles/0")).isEqualTo(profile);
        assertThat(listed.at("/result/structuredContent/result/updatedAt").asText()).isEqualTo(version);
        var found = mcp("tools/call", Map.of("name", "finance_get_loan_profile", "arguments",
                Map.of("profileId", profile.path("id").asText())), "native-agent");
        assertThat(found.at("/result/structuredContent/result/profile")).isEqualTo(profile);
        var requestId = UUID.randomUUID().toString();
        var saved = mcp("tools/call", Map.of("name", "finance_save_loan_profile", "arguments",
                Map.of("requestId", requestId, "profile", json.convertValue(profile, Map.class),
                        "expectedUpdatedAt", version)), "native-agent");
        assertThat(saved.at("/result/isError").asBoolean()).as("Save response: %s", saved).isFalse();
        assertThat(saved.has("error")).as("Save response: %s", saved).isFalse();
        var sent = BACKEND.requests().getLast();
        assertThat(sent.method()).isEqualTo("PUT");
        assertThat(sent.path()).isEqualTo("/api/finance/v1/review");
        assertThat(sent.requestId()).isEqualTo(requestId);
        assertThat(sent.body().at("/loanProfileChange/profile")).isEqualTo(profile);
        assertThat(sent.body().path("expectedUpdatedAt").asText()).isEqualTo(version);
        var beforeDeletion = BACKEND.requests().size();
        mcp("tools/call", Map.of("name", "finance_remove_loan_profile", "arguments",
                Map.of("requestId", UUID.randomUUID().toString(), "profileId", profile.path("id").asText(),
                        "expectedUpdatedAt", version, "confirmed", false)), "native-agent");
        assertThat(BACKEND.requests()).hasSize(beforeDeletion);
        var removed = mcp("tools/call", Map.of("name", "finance_remove_loan_profile", "arguments",
                Map.of("requestId", UUID.randomUUID().toString(), "profileId", profile.path("id").asText(),
                        "expectedUpdatedAt", version, "confirmed", true)), "native-agent");
        assertThat(removed.has("error")).isFalse();
        assertThat(removed.at("/result/isError").asBoolean()).isFalse();
        var deletion = BACKEND.requests().getLast();
        assertThat(deletion.body().at("/loanProfileChange/operation").asText()).isEqualTo("remove");
        assertThat(deletion.body().at("/loanProfileChange/profileId").asText()).isEqualTo(profile.path("id").asText());
        assertThat(deletion.body().at("/loanProfileChange/confirmed").asBoolean()).isTrue();
        assertThat(deletion.body().path("expectedUpdatedAt").asText()).isEqualTo(version);
    }

    @Test
    void settingsApprovalsRequireBrowserCsrfStayWithinTheAccountAndRevokeOnlyTheSelectedDomain() throws Exception {
        var registered = body(postJson("/oauth2/register", """
                {"client_name":"Settings agent","redirect_uris":["https://agent.example/callback"],
                 "token_endpoint_auth_method":"none","scope":"mcp"}
                """));
        var clientId = registered.path("client_id").asText();
        assertThat(clientId).isNotBlank();
        signIn();
        var approval = json.writeValueAsString(Map.of("clientId", clientId, "clientName", "My agent"));
        assertThat(postJson("/agents/inventory/clients", approval).statusCode()).isEqualTo(403);
        var csrf = body(get("/csrf"));
        var request = HttpRequest.newBuilder(url("/agents/inventory/clients"))
                .header("Content-Type", "application/json").header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(approval)).build();
        var approved = browser.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(approved.statusCode()).isEqualTo(200);
        assertThat(body(approved).path("revokedAt").isNull()).isTrue();
        assertThat(body(get("/agents/inventory/clients")).findValues("clientId").stream().map(JsonNode::asText).toList()).contains(clientId);
        assertThat(body(get("/agents/finance/clients")).findValues("clientId").stream().map(JsonNode::asText).toList()).doesNotContain(clientId);
        var revoked = browser.send(HttpRequest.newBuilder(url("/agents/inventory/clients/" + encode(clientId)))
                .header(csrf.get("headerName").asText(), csrf.get("token").asText()).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(revoked.statusCode()).isEqualTo(200);
        assertThat(body(revoked).path("revokedAt").isString()).isTrue();
        var otherUser = UUID.randomUUID();
        database.update("insert into public.yourlifeone_finance_oauth_clients (user_id,client_id,client_name) values (?,?,?)",
                otherUser, clientId, "Other account");
        assertThat(body(get("/agents/finance/clients")).findValues("clientId").stream().map(JsonNode::asText).toList()).doesNotContain(clientId);
    }

    @Test
    void mcpRejectsBrowserCookiesAndBrowserAccessTokens() throws Exception {
        signIn();
        var token = body(get("/session")).path("accessToken").asText();
        var builder = HttpRequest.newBuilder(url("/mcp")).header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}"));
        assertThat(browser.send(builder.build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        assertThat(browser.send(builder.header("Authorization", "Bearer " + token).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        assertThat(BACKEND.requests()).isEmpty();
    }

    private String signIn() throws Exception {
        URI next = url("/signin");
        for (int step = 0; step < 10; step++) {
            var response = browser.send(HttpRequest.newBuilder(next).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isBetween(300, 399);
            next = next.resolve(response.headers().firstValue("Location").orElseThrow());
            if (next.toString().startsWith(BROWSER)) {
                return next.toString();
            }
        }
        throw new AssertionError("Sign-in did not reach the configured browser URL");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var response = browser.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private JsonNode body(HttpResponse<String> response) throws Exception {
        return json.readTree(response.body());
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        return browser.send(HttpRequest.newBuilder(url(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode mcp(String method, Map<String, Object> parameters, String clientId) throws Exception {
        return mcp(method, parameters, clientId, null);
    }

    private JsonNode mcp(String method, Map<String, Object> parameters, String clientId, String endpoint) throws Exception {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).subject(USER.toString())
                .audience(List.of("authenticated", ISSUER + "/mcp")).issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("client_id", clientId).claim("scope", "mcp").build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(SIGNING_KEY.getKeyID()).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        var builder = HttpRequest.newBuilder(url("/mcp")).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2025-11-25")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                        Map.of("jsonrpc", "2.0", "id", 1, "method", method, "params", parameters))));
        if (endpoint != null) builder.header("X-Product-Mcp-Domain", endpoint);
        var response = browser.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        if (response.headers().firstValue("Content-Type").orElse("").contains("text/event-stream")) {
            var message = response.body().lines().filter(line -> line.startsWith("data: ")).reduce((first, last) -> last).orElseThrow();
            return json.readTree(message.substring(6));
        }
        return json.readTree(response.body());
    }

    private HttpResponse<String> postForm(String path, String form, JsonNode csrf) throws Exception {
        var request = HttpRequest.newBuilder(url(path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        if (csrf != null) {
            request.header(csrf.get("headerName").asText(), csrf.get("token").asText());
        }
        return browser.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parameters(String encoded) {
        return Arrays.stream(encoded.split("&")).map(part -> part.split("=", 2))
                .collect(Collectors.toMap(pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + port + "/api/access" + path);
    }
}
