package com.yourlifeone.commonservice.testing;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Synthetic ES256 issuer: exercises the production JWKS decoder and claim validators over HTTP. */
public final class AccessTokens implements AutoCloseable {
    private final ECKey key;
    private final HttpServer server;

    public AccessTokens() throws JOSEException, IOException {
        key = new ECKeyGenerator(Curve.P_256).keyID("access-test").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var output = exchange.getResponseBody()) {
                output.write(jwks);
            }
        });
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String token(String subject, String client) throws JOSEException {
        return token(subject, client, "authenticated", baseUrl() + "/auth/v1", Instant.now().plusSeconds(300));
    }

    public String token(String subject, String client, String audience, String issuer, Instant expiry)
            throws JOSEException {
        var claims = new JWTClaimsSet.Builder().subject(subject).issuer(issuer).audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(600))).expirationTime(Date.from(expiry));
        if (client != null) claims.claim("client_id", client);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
