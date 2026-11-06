package com.yourlifeone.access.identity.application;

import com.yourlifeone.access.identity.infrastructure.persistence.AccountIdentity;
import com.yourlifeone.access.identity.infrastructure.persistence.AccountIdentityRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountIdentityResolverTest {
    private final AccountIdentityRepository identities = mock(AccountIdentityRepository.class);
    private final AccountIdentityResolver resolver = new AccountIdentityResolver(identities);

    @Test
    void verifiedGoogleSubjectResolvesToTheExistingAccountUuid() {
        var google = mock(OidcUser.class);
        when(google.getEmailVerified()).thenReturn(true);
        when(google.getSubject()).thenReturn("synthetic-google-subject");
        var identity = mock(AccountIdentity.class);
        var existingId = UUID.randomUUID();
        when(identity.getUserId()).thenReturn(existingId);
        when(identity.getEmail()).thenReturn("existing@example.test");
        when(identities.findByProviderAndSubjectAndEnabledTrue("google", "synthetic-google-subject"))
                .thenReturn(Optional.of(identity));

        var account = resolver.resolve(google);

        assertThat(account.userId()).isEqualTo(existingId);
        assertThat(account.email()).isEqualTo("existing@example.test");
        verify(identities, never()).save(identity);
    }

    @Test
    void matchingEmailDoesNotLinkAnUnknownGoogleSubject() {
        var google = mock(OidcUser.class);
        when(google.getEmailVerified()).thenReturn(true);
        when(google.getSubject()).thenReturn("unknown-subject");
        when(google.getEmail()).thenReturn("existing@example.test");
        when(identities.findByProviderAndSubjectAndEnabledTrue("google", "unknown-subject"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(google)).isInstanceOf(OAuth2AuthenticationException.class);
        verify(google, never()).getEmail();
    }

    @Test
    void unverifiedEmailIsRefusedBeforeLookingUpAnyAccount() {
        var google = mock(OidcUser.class);
        when(google.getEmailVerified()).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolve(google)).isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoInteractions(identities);
    }
}
