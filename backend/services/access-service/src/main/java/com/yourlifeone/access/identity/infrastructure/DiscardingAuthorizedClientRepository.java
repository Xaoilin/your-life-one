package com.yourlifeone.access.identity.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/** Identity-only Google sign-in does not retain a Google API credential. Calendar has its own grant. */
public class DiscardingAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {
    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String registrationId,
            Authentication principal, HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal,
            HttpServletRequest request, HttpServletResponse response) {
        // Identity has been verified; no provider credential needs persistence.
    }

    @Override
    public void removeAuthorizedClient(String registrationId, Authentication principal,
            HttpServletRequest request, HttpServletResponse response) {
        // Nothing was retained.
    }
}
