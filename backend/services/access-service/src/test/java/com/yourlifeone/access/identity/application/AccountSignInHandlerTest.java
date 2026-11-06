package com.yourlifeone.access.identity.application;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountSignInHandlerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sessionContainsOnlyProductIdentityAndRedirectCannotBeChosenByTheRequest() throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("synthetic").generate();
        var properties = AccessProperties.builder()
                .publicUrl(URI.create("https://api.yourlife.example/api/access"))
                .browserUrl(URI.create("https://yourlife.example/yourlifeone/"))
                .allowedOrigins(List.of("https://yourlife.example"))
                .signingKeys(new JWKSet(key).toString(false)).signingKeyId(key.getKeyID())
                .audience("authenticated").accessTokenLifetime(Duration.ofMinutes(10)).build();
        var resolver = mock(AccountIdentityResolver.class);
        var google = mock(OidcUser.class);
        var account = new AccountPrincipal(UUID.randomUUID(), "existing@example.test");
        when(resolver.resolve(google)).thenReturn(account);
        var handler = new AccountSignInHandler(resolver, properties);
        var request = new MockHttpServletRequest();
        request.addParameter("redirectTo", "https://attacker.example");
        var response = new MockHttpServletResponse();
        var authentication = new OAuth2AuthenticationToken(google,
                AuthorityUtils.createAuthorityList("ROLE_USER"), "google");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(303);
        assertThat(response.getHeader("Location")).isEqualTo("https://yourlife.example/yourlifeone/");
        var stored = (SecurityContext) request.getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored.getAuthentication().getPrincipal()).isEqualTo(account);
        assertThat(stored.getAuthentication().getName()).isEqualTo(account.userId().toString());
        assertThat(stored.getAuthentication().getCredentials()).isNull();
        assertThat(stored.getAuthentication().getPrincipal()).isNotInstanceOf(OidcUser.class);
    }
}
