package com.yourlifeone.access.identity.application;

import com.yourlifeone.access.identity.domain.AccountPrincipal;
import com.yourlifeone.access.identity.infrastructure.persistence.AccountIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A verified Google identity must match an existing imported identity, never just an email address. */
@Service
@RequiredArgsConstructor
public class AccountIdentityResolver {
    private final AccountIdentityRepository identities;

    @Transactional(readOnly = true)
    public AccountPrincipal resolve(OidcUser google) {
        if (!Boolean.TRUE.equals(google.getEmailVerified())) {
            throw denied();
        }
        var identity = identities.findByProviderAndSubjectAndEnabledTrue("google", google.getSubject())
                .orElseThrow(AccountIdentityResolver::denied);
        return new AccountPrincipal(identity.getUserId(), identity.getEmail());
    }

    private static OAuth2AuthenticationException denied() {
        return new OAuth2AuthenticationException(new OAuth2Error(
                "account_not_allowed", "This identity is not enabled for this product.", null));
    }
}
