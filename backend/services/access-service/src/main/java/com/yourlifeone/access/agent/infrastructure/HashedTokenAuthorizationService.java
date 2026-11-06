package com.yourlifeone.access.agent.infrastructure;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.Assert;

/**
 * Keeps only a hash of each authorization code, access token and refresh token in the authorizations table, so that a
 * copy of the database holds nothing that can be presented to this service. Access tokens still work without the
 * table, because the resource server checks their signature; the stored value is only needed to revoke or introspect.
 *
 * <ul>
 *   <li>Saving replaces each token's value with its hash.</li>
 *   <li>Looking a token up hashes the presented value first, always, so a stored hash is never accepted as a token.</li>
 *   <li>The authorization that comes back carries the presented value again for the token that matched. Spring's
 *       revocation and introspection find that token by its presented value, and a refresh that reuses the token
 *       hands the value back to the client; both would otherwise see the hash.</li>
 *   <li>Every other token in a loaded authorization still holds its stored hash. Saving leaves values that are
 *       already hashes alone, so loading and saving again never hashes twice.</li>
 * </ul>
 *
 * The consent {@code state} is not a token and is looked up as given. This service issues no ID tokens, user codes or
 * device codes; if OpenID Connect or the device flow is ever enabled, hash those here too.
 */
public class HashedTokenAuthorizationService implements OAuth2AuthorizationService {

    private static final List<Class<? extends OAuth2Token>> HASHED_TOKEN_TYPES =
            List.of(OAuth2AuthorizationCode.class, OAuth2AccessToken.class, OAuth2RefreshToken.class);

    private final OAuth2AuthorizationService delegate;
    private final TokenHasher hasher;

    public HashedTokenAuthorizationService(OAuth2AuthorizationService delegate, TokenHasher hasher) {
        this.delegate = delegate;
        this.hasher = hasher;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(withHashedTokens(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        if (isState(tokenType)) {
            return delegate.findByToken(token, tokenType);
        }
        OAuth2Authorization stored = delegate.findByToken(hasher.hash(token), tokenType);
        return stored == null ? null : withPresentedToken(stored, token);
    }

    private static boolean isState(OAuth2TokenType tokenType) {
        return tokenType != null && OAuth2ParameterNames.STATE.equals(tokenType.getValue());
    }

    private OAuth2Authorization withHashedTokens(OAuth2Authorization authorization) {
        OAuth2Authorization.Builder hashed = OAuth2Authorization.from(authorization);
        HASHED_TOKEN_TYPES.forEach(type -> hashToken(authorization, type, hashed));
        return hashed.build();
    }

    private <T extends OAuth2Token> void hashToken(
            OAuth2Authorization authorization, Class<T> type, OAuth2Authorization.Builder hashed) {
        OAuth2Authorization.Token<T> held = authorization.getToken(type);
        if (held == null) {
            return;
        }
        String value = held.getToken().getTokenValue();
        if (!hasher.isHashed(value)) {
            hashed.token(withValue(held.getToken(), hasher.hash(value)));
        }
    }

    private OAuth2Authorization withPresentedToken(OAuth2Authorization stored, String presented) {
        OAuth2Authorization.Token<OAuth2Token> matched = stored.getToken(hasher.hash(presented));
        if (matched == null) {
            return stored;
        }
        return OAuth2Authorization.from(stored).token(withValue(matched.getToken(), presented)).build();
    }

    /** The same kind of token with another value; the builder keeps the token's metadata, such as revoked. */
    private static OAuth2Token withValue(OAuth2Token token, String value) {
        return switch (token) {
            case OAuth2AuthorizationCode code ->
                    new OAuth2AuthorizationCode(value, code.getIssuedAt(), code.getExpiresAt());
            case OAuth2AccessToken access -> new OAuth2AccessToken(
                    access.getTokenType(), value, access.getIssuedAt(), access.getExpiresAt(), access.getScopes());
            case OAuth2RefreshToken refresh ->
                    new OAuth2RefreshToken(value, refresh.getIssuedAt(), refresh.getExpiresAt());
            default -> throw new IllegalArgumentException(
                    "No hashing rule for " + token.getClass().getSimpleName());
        };
    }
}
