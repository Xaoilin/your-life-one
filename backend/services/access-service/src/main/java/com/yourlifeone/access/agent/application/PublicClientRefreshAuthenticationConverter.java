package com.yourlifeone.access.agent.application;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * Recognises a public client (no secret) asking to refresh its tokens. Spring authenticates public clients only on the
 * first sign-in, by the PKCE proof; a refresh has no PKCE proof, so without this the client would be turned away.
 * The refresh token itself is the proof of possession, and the refresh grant checks it was issued to this client.
 */
public class PublicClientRefreshAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod()) || !isRefreshGrant(request) || carriesOtherCredentials(request)) {
            return null;
        }
        String[] clientIds = request.getParameterValues(OAuth2ParameterNames.CLIENT_ID);
        if (clientIds == null || clientIds.length != 1 || !StringUtils.hasText(clientIds[0])) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "A public client must send exactly one client_id to refresh its tokens", null));
        }
        return new OAuth2ClientAuthenticationToken(clientIds[0], ClientAuthenticationMethod.NONE, null,
                Map.of(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue()));
    }

    private static boolean isRefreshGrant(HttpServletRequest request) {
        return AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));
    }

    /** A client sending a secret or assertion is a confidential client and is handled by Spring's own converters. */
    private static boolean carriesOtherCredentials(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION) != null
                || request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null
                || request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION) != null;
    }
}
