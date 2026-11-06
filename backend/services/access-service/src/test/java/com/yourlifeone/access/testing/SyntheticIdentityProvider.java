package com.yourlifeone.access.testing;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** A local signed OIDC provider. No Google account, network grant or real credential is used. */
public final class SyntheticIdentityProvider implements AutoCloseable {
    private final HttpServer server;
    private final RSAKey key;
    private final Map<String, String> nonces = new ConcurrentHashMap<>();
    private volatile String subject = "existing-google-subject";

    public SyntheticIdentityProvider() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("synthetic-provider").generate();
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
        server.createContext("/.well-known/openid-configuration", exchange -> json(exchange, metadata()));
        server.createContext("/authorize", this::authorize);
        server.createContext("/token", this::token);
        server.createContext("/jwks", exchange -> json(exchange, new JWKSet(key.toPublicJWK()).toString()));
        server.createContext("/userinfo", exchange -> json(exchange,
                "{\"sub\":\"" + subject + "\",\"email\":\"synthetic@example.test\",\"email_verified\":true}"));
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void subject(String nextSubject) {
        subject = nextSubject;
    }

    private String metadata() {
        return "{\"issuer\":\"" + url() + "\","
                + "\"authorization_endpoint\":\"" + url() + "/authorize\","
                + "\"token_endpoint\":\"" + url() + "/token\","
                + "\"userinfo_endpoint\":\"" + url() + "/userinfo\","
                + "\"jwks_uri\":\"" + url() + "/jwks\","
                + "\"response_types_supported\":[\"code\"],\"subject_types_supported\":[\"public\"],"
                + "\"id_token_signing_alg_values_supported\":[\"RS256\"],"
                + "\"token_endpoint_auth_methods_supported\":[\"client_secret_basic\",\"client_secret_post\"]}";
    }

    private void authorize(HttpExchange exchange) throws IOException {
        var request = parameters(exchange.getRequestURI().getRawQuery());
        var code = UUID.randomUUID().toString();
        nonces.put(code, request.get("nonce"));
        var callback = request.get("redirect_uri") + "?code=" + code
                + "&state=" + URLEncoder.encode(request.get("state"), StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Location", callback);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void token(HttpExchange exchange) throws IOException {
        var request = parameters(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        var nonce = nonces.remove(request.get("code"));
        if (nonce == null) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }
        try {
            var now = Instant.now();
            var claims = new JWTClaimsSet.Builder().issuer(url()).subject(subject).audience("synthetic-client")
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)))
                    .claim("nonce", nonce).claim("email", "synthetic@example.test")
                    .claim("email_verified", true).build();
            var signed = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            signed.sign(new RSASSASigner(key));
            json(exchange, "{\"access_token\":\"synthetic-access\",\"token_type\":\"Bearer\","
                    + "\"expires_in\":300,\"id_token\":\"" + signed.serialize() + "\"}");
        } catch (Exception failure) {
            throw new IOException("Synthetic token creation failed", failure);
        }
    }

    private static Map<String, String> parameters(String encoded) {
        return Arrays.stream(encoded.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(pair -> decode(pair[0]), pair -> decode(pair[1])));
    }

    private static String decode(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private static void json(HttpExchange exchange, String json) throws IOException {
        var bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
