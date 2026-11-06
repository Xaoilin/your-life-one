package com.yourlifeone.commonservice.security;

import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Claim checks for the configured product's access tokens, on top of the signature check. */
public final class SupabaseTokenValidators {
    /** Supabase puts this audience on tokens of signed-in users (not on the anonymous key). */
    static final String SIGNED_IN_AUDIENCE = "authenticated";

    private SupabaseTokenValidators() {
    }

    /** Checks expiry, the Supabase Auth issuer, and the signed-in user audience. */
    public static OAuth2TokenValidator<Jwt> forIssuer(String issuer) {
        return forIssuer(issuer, SIGNED_IN_AUDIENCE);
    }

    /** The provider switch never relaxes issuer, expiry or audience checks. */
    public static OAuth2TokenValidator<Jwt> forIssuer(String issuer, String audience) {
        var audienceValidator = new JwtClaimValidator<List<String>>("aud",
                audiences -> audiences != null && audiences.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
    }
}
