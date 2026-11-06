package com.yourlifeone.access.agent.application;

import com.yourlifeone.access.agent.domain.RedirectUriPolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.util.StringUtils;

/**
 * Command-line AI tools listen on a random local port and call themselves {@code http://localhost:PORT/...}. Spring
 * already lets the port vary for IP-literal loopback addresses; this extends that to the name {@code localhost}
 * (RFC 8252 section 7.3). Host and path must still match a registered address exactly; every other address uses
 * Spring's exact-match rule.
 */
public class LoopbackRedirectUriValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private static final String LOCALHOST = "localhost";
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1";

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        URI requested = parse(request.getRedirectUri());
        if (requested != null && LOCALHOST.equalsIgnoreCase(requested.getHost())) {
            requireRegisteredLoopback(context, request, requested);
            return;
        }
        OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR.accept(context);
    }

    private void requireRegisteredLoopback(
            OAuth2AuthorizationCodeRequestAuthenticationContext context,
            OAuth2AuthorizationCodeRequestAuthenticationToken request,
            URI requested) {
        boolean registered = requested.getFragment() == null && context.getRegisteredClient().getRedirectUris().stream()
                .map(LoopbackRedirectUriValidator::parse)
                .anyMatch(candidate -> candidate != null && sameApartFromPort(candidate, requested));
        if (!registered) {
            throw redirectUriError(request);
        }
    }

    private static boolean sameApartFromPort(URI registered, URI requested) {
        return registered.getScheme() != null
                && registered.getScheme().equalsIgnoreCase(requested.getScheme())
                && registered.getHost() != null
                && registered.getHost().toLowerCase(Locale.ROOT).equals(requested.getHost().toLowerCase(Locale.ROOT))
                && RedirectUriPolicy.isLoopbackHost(registered.getHost())
                && StringUtils.hasText(registered.getRawPath()) == StringUtils.hasText(requested.getRawPath())
                && (registered.getRawPath() == null || registered.getRawPath().equals(requested.getRawPath()))
                && Objects.equals(registered.getRawQuery(), requested.getRawQuery());
    }

    private static OAuth2AuthorizationCodeRequestAuthenticationException redirectUriError(
            OAuth2AuthorizationCodeRequestAuthenticationToken request) {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: " + OAuth2ParameterNames.REDIRECT_URI, ERROR_URI);
        OAuth2AuthorizationCodeRequestAuthenticationToken result = new OAuth2AuthorizationCodeRequestAuthenticationToken(
                request.getAuthorizationUri(),
                request.getClientId(),
                (Authentication) request.getPrincipal(),
                null,
                request.getState(),
                request.getScopes(),
                request.getAdditionalParameters());
        result.setAuthenticated(true);
        return new OAuth2AuthorizationCodeRequestAuthenticationException(error, result);
    }

    private static URI parse(String uri) {
        try {
            return StringUtils.hasText(uri) ? new URI(uri) : null;
        } catch (URISyntaxException invalid) {
            return null;
        }
    }
}
