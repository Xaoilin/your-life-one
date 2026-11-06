package com.yourlifeone.access.agent.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * Issues refresh tokens to every client, including public ones that sign in with PKCE and no secret. Spring's default
 * generator withholds a refresh token from such clients on the first sign-in, which would make every AI tool ask the
 * customer to sign in again each time the short-lived access token expires. The risk of a refresh token held by a
 * public client is limited by rotation: each use replaces the token (see the client registration policy).
 */
public class PublicClientRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private static final int TOKEN_BITS = 96;

    private final StringKeyGenerator keys = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), TOKEN_BITS);
    private final Clock clock = Clock.systemUTC();

    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        return new OAuth2RefreshToken(keys.generateKey(), issuedAt, expiresAt);
    }
}
