package com.yourlifeone.commonservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.util.StringUtils;

/**
 * The one identity provider trusted by this product's APIs. Explicit issuer and JWKS settings let
 * the product switch providers without trusting both products or accepting unsigned tokens.
 * The Supabase URL is a temporary compatibility input; remove it after both product cutovers.
 */
@ConfigurationProperties("app.auth")
public record IdentityProviderProperties(
        String supabaseUrl,
        String issuerUri,
        String jwkSetUri,
        String audience,
        SignatureAlgorithm signatureAlgorithm) {

    @ConstructorBinding
    public IdentityProviderProperties {
        boolean explicitIssuer = StringUtils.hasText(issuerUri);
        boolean explicitKeys = StringUtils.hasText(jwkSetUri);
        if (explicitIssuer != explicitKeys) {
            throw new IllegalArgumentException("Configure both app.auth.issuer-uri and app.auth.jwk-set-uri.");
        }
        if (!explicitIssuer && !StringUtils.hasText(supabaseUrl)) {
            throw new IllegalArgumentException("An identity provider must be configured.");
        }
        audience = StringUtils.hasText(audience) ? audience : "authenticated";
        signatureAlgorithm = signatureAlgorithm == null ? SignatureAlgorithm.ES256 : signatureAlgorithm;
        if (signatureAlgorithm != SignatureAlgorithm.ES256 && signatureAlgorithm != SignatureAlgorithm.RS256) {
            throw new IllegalArgumentException("Only ES256 and RS256 identity signing keys are supported.");
        }
    }

    public IdentityProviderProperties(String supabaseUrl) {
        this(supabaseUrl, null, null, null, null);
    }

    public String issuer() {
        if (StringUtils.hasText(issuerUri)) {
            return issuerUri;
        }
        return supabaseUrl.replaceAll("/+$", "") + "/auth/v1";
    }

    @Override
    public String jwkSetUri() {
        return StringUtils.hasText(jwkSetUri) ? jwkSetUri : issuer() + "/.well-known/jwks.json";
    }
}
