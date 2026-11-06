package com.yourlifeone.access.agent.infrastructure;

import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * Stores only the customer's id and authorities with each OAuth authorization, instead of the whole sign-in. Issuing
 * and refreshing tokens needs nothing more, and the authorizations table should not hold a copy of the customer's email
 * address or session details that account deletion would then have to find and remove.
 */
public class MinimalPrincipalAuthorizationService implements OAuth2AuthorizationService {

    private static final String PRINCIPAL_ATTRIBUTE = Principal.class.getName();

    private final OAuth2AuthorizationService delegate;

    public MinimalPrincipalAuthorizationService(OAuth2AuthorizationService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(withMinimalPrincipal(authorization));
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
        return delegate.findByToken(token, tokenType);
    }

    private static OAuth2Authorization withMinimalPrincipal(OAuth2Authorization authorization) {
        Authentication principal = authorization.getAttribute(PRINCIPAL_ATTRIBUTE);
        if (principal == null || (principal instanceof UsernamePasswordAuthenticationToken
                && principal.getPrincipal() instanceof String)) {
            return authorization;
        }
        Authentication minimal = UsernamePasswordAuthenticationToken.authenticated(
                principal.getName(), null, principal.getAuthorities());
        return OAuth2Authorization.from(authorization).attribute(PRINCIPAL_ATTRIBUTE, minimal).build();
    }
}
