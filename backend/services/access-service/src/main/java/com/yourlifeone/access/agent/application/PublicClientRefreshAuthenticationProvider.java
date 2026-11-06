package com.yourlifeone.access.agent.application;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Accepts a public client on the refresh grant when it is registered as one and may use refresh tokens. Anything else
 * returns nothing, so Spring's own providers decide.
 */
public class PublicClientRefreshAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository clients;

    public PublicClientRefreshAuthenticationProvider(RegisteredClientRepository clients) {
        this.clients = clients;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        OAuth2ClientAuthenticationToken request = (OAuth2ClientAuthenticationToken) authentication;
        if (!isPublicRefreshRequest(request)) {
            return null;
        }
        RegisteredClient client = clients.findByClientId(request.getPrincipal().toString());
        if (client == null
                || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                || !client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT));
        }
        return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static boolean isPublicRefreshRequest(OAuth2ClientAuthenticationToken request) {
        return ClientAuthenticationMethod.NONE.equals(request.getClientAuthenticationMethod())
                && AuthorizationGrantType.REFRESH_TOKEN.getValue()
                        .equals(request.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE));
    }
}
