package com.yourlifeone.access.identity.application;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.identity.api.controller.SigningKeysController;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserAccessTokensTest {
    @Test
    void tokenKeepsTheMigratedUuidAndProductIssuerWithoutAgentPrivileges() throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("partner-test").generate();
        var keys = new JWKSet(key);
        var properties = AccessProperties.builder()
                .publicUrl(URI.create("https://api.yourlife.example/api/access"))
                .browserUrl(URI.create("https://yourlife.example/yourlifeone/"))
                .allowedOrigins(List.of("https://yourlife.example"))
                .signingKeys(keys.toString(false)).signingKeyId(key.getKeyID())
                .audience("authenticated").accessTokenLifetime(Duration.ofMinutes(10)).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(keys));
        var now = Instant.now();
        var tokens = new BrowserAccessTokens(properties, encoder, Clock.fixed(now, ZoneOffset.UTC));
        var account = new AccountPrincipal(UUID.randomUUID(), "synthetic@example.test");

        var response = tokens.issue(account);
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.publicUrl().toString()));
        var verified = decoder.decode(response.accessToken());

        assertThat(verified.getSubject()).isEqualTo(account.userId().toString());
        assertThat(verified.getAudience()).containsExactly("authenticated");
        assertThat(verified.getClaimAsString("role")).isEqualTo("authenticated");
        assertThat(verified.getClaims()).doesNotContainKeys("client_id", "email", "scope", "refresh_token");
        assertThat(response.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(10)));
        assertThat(response.user().id()).isEqualTo(account.userId());
        assertThat(account.getName()).isEqualTo(account.userId().toString());

        var publicKeys = JWKSet.parse(new SigningKeysController(keys).keys());
        assertThat(publicKeys.getKeyByKeyId("partner-test").isPrivate()).isFalse();
    }
}
