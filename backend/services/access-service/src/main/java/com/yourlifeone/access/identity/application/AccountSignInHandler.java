package com.yourlifeone.access.identity.application;

import com.yourlifeone.access.config.AccessProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/** Resolves the migrated UUID, discards upstream tokens, and saves only the minimal product identity. */
@Component
@RequiredArgsConstructor
public class AccountSignInHandler implements AuthenticationSuccessHandler {
    private final AccountIdentityResolver identities;
    private final AccessProperties properties;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser google)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        var account = identities.resolve(google);
        var signedIn = UsernamePasswordAuthenticationToken.authenticated(
                account, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(signedIn);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        var requests = new HttpSessionRequestCache();
        var saved = requests.getRequest(request, response);
        var target = properties.browserUrl().toString();
        if (saved != null) {
            var original = URI.create(saved.getRedirectUrl());
            // Resume only a local authorization request, rebuilding its origin from trusted configuration.
            if (original.getPath().equals(request.getContextPath() + "/oauth2/authorize")) {
                target = properties.publicUrl() + "/oauth2/authorize"
                        + (original.getRawQuery() == null ? "" : "?" + original.getRawQuery());
            }
            requests.removeRequest(request, response);
        }
        response.setHeader("Location", target);
    }
}
