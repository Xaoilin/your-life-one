package com.yourlifeone.access.identity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Rejects unknown identities before Spring establishes an authenticated browser session. */
@Component
@RequiredArgsConstructor
public class ExistingAccountOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final AccountIdentityResolver identities;
    private final OidcUserService provider = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser user = provider.loadUser(request);
        identities.resolve(user);
        return user;
    }
}
