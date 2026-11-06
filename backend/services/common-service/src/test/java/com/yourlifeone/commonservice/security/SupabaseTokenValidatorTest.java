package com.yourlifeone.commonservice.security;

import com.yourlifeone.commonservice.web.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseTokenValidatorTest {
    private static final IdentityProviderProperties PROPERTIES =
            new IdentityProviderProperties("https://project.supabase.co/");
    private static final UUID USER = UUID.randomUUID();

    private static Jwt token(String issuer, String audience, Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .subject(USER.toString())
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }

    private static boolean isValid(Jwt jwt) {
        return !SupabaseTokenValidators.forIssuer(PROPERTIES.issuer()).validate(jwt).hasErrors();
    }

    @Test
    void derivesTheIssuerAndKeySetFromTheProjectUrl() {
        assertEquals("https://project.supabase.co/auth/v1", PROPERTIES.issuer());
        assertEquals("https://project.supabase.co/auth/v1/.well-known/jwks.json", PROPERTIES.jwkSetUri());
    }

    @Test
    void acceptsASignedInUserToken() {
        var jwt = token(PROPERTIES.issuer(), "authenticated", Instant.now().plusSeconds(60));

        assertTrue(isValid(jwt));
        assertEquals(USER, AuthenticatedUser.id(jwt));
    }

    @Test
    void rejectsAnotherIssuerTheAnonymousAudienceAndExpiredTokens() {
        assertFalse(isValid(token("https://other.supabase.co/auth/v1", "authenticated", Instant.now().plusSeconds(60))));
        assertFalse(isValid(token(PROPERTIES.issuer(), "anon", Instant.now().plusSeconds(60))));
        assertFalse(isValid(token(PROPERTIES.issuer(), "authenticated", Instant.now().minusSeconds(120))));
    }

    @Test
    void rejectsASubjectThatIsNotAUserId() {
        var jwt = Jwt.withTokenValue("token").header("alg", "ES256").subject("service").build();

        assertThrows(ApiException.class, () -> AuthenticatedUser.id(jwt));
    }

    @Test
    void nativeIdentityAcceptsOnlyItsOwnIssuerAndAudience() {
        var properties = new IdentityProviderProperties(
                null, "https://api.partner.example/api/auth", "https://api.partner.example/api/auth/oauth2/jwks",
                "partner-one", SignatureAlgorithm.RS256);
        var validator = SupabaseTokenValidators.forIssuer(properties.issuer(), properties.audience());
        var expiry = Instant.now().plusSeconds(60);

        assertFalse(validator.validate(token(properties.issuer(), "partner-one", expiry)).hasErrors());
        assertTrue(validator.validate(token("https://api.yourlife.example/api/auth", "partner-one", expiry)).hasErrors());
        assertTrue(validator.validate(token(properties.issuer(), "your-life-one", expiry)).hasErrors());
        assertTrue(validator.validate(token(PROPERTIES.issuer(), "authenticated", expiry)).hasErrors());
        assertEquals("https://api.partner.example/api/auth/oauth2/jwks", properties.jwkSetUri());
    }

    @Test
    void refusesAnIncompleteProviderSwitchAndUnsafeSigningAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityProviderProperties(
                PROPERTIES.supabaseUrl(), "https://api.partner.example/api/auth", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new IdentityProviderProperties(
                null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new IdentityProviderProperties(
                null, "https://api.partner.example/api/auth", "https://api.partner.example/api/auth/oauth2/jwks",
                "partner-one", SignatureAlgorithm.RS512));
    }
}
