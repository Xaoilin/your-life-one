package com.yourlifeone.access.agent.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.yourlifeone.access.agent.application.ClientRegistrationPolicy;
import com.yourlifeone.access.agent.application.AgentEndpointLimits;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import com.yourlifeone.access.agent.application.LoopbackRedirectUriValidator;
import com.yourlifeone.access.agent.application.PublicClientRefreshAuthenticationConverter;
import com.yourlifeone.access.agent.application.PublicClientRefreshAuthenticationProvider;
import com.yourlifeone.access.agent.infrastructure.HashedTokenAuthorizationService;
import com.yourlifeone.access.agent.infrastructure.MinimalPrincipalAuthorizationService;
import com.yourlifeone.access.agent.infrastructure.PublicClientRefreshTokenGenerator;
import com.yourlifeone.access.agent.infrastructure.TokenHasher;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/** Product-owned OAuth authorization server: PKCE, explicit consent, bounded tokens and persistent grants. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAuthorizationConfiguration {
    @Bean
    @Order(1)
    SecurityFilterChain authorizationSecurityFilterChain(HttpSecurity http,
            ClientRegistrationPolicy policy, RegisteredClientRepository clients) throws Exception {
        http.cors(Customizer.withDefaults());
        http.addFilterBefore(new AgentEndpointLimits(), SecurityContextHolderFilter.class);
        http.oauth2AuthorizationServer(server -> {
            http.securityMatcher(server.getEndpointsMatcher());
            server.clientAuthentication(client -> client
                    .authenticationConverter(new PublicClientRefreshAuthenticationConverter())
                    .authenticationProvider(new PublicClientRefreshAuthenticationProvider(clients)));
            server.authorizationEndpoint(endpoint -> endpoint.consentPage("/oauth2/consent").authenticationProviders(providers -> {
                for (var provider : providers) {
                    if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider code) {
                        code.setAuthenticationValidator(new LoopbackRedirectUriValidator()
                                .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR));
                    }
                }
            }));
            server.authorizationServerMetadataEndpoint(metadata ->
                    metadata.authorizationServerMetadataCustomizer(policy.metadataCustomizer()));
            server.clientRegistrationEndpoint(registration -> registration.openRegistrationAllowed(true)
                    .authenticationProviders(providers -> {
                        for (var provider : providers) {
                            if (provider instanceof OAuth2ClientRegistrationAuthenticationProvider client) {
                                client.setAuthenticationValidator(policy.validator());
                                client.setRegisteredClientConverter(policy.converter());
                            }
                        }
                    }));
        });
        // Agent bearer tokens must never substitute for the person's browser session at consent.
        http.oauth2ResourceServer(resource -> resource.bearerTokenResolver(request -> null));
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.POST, "/oauth2/register").permitAll()
                .anyRequest().authenticated());
        http.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/signin"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    @Bean
    ClientRegistrationPolicy clientRegistrationPolicy(AgentProperties properties) {
        return new ClientRegistrationPolicy(properties);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(AccessProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.publicUrl().toString()).build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate database) {
        return new JdbcRegisteredClientRepository(database);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(JdbcTemplate database, RegisteredClientRepository clients) {
        return new HashedTokenAuthorizationService(
                new MinimalPrincipalAuthorizationService(new JdbcOAuth2AuthorizationService(database, clients)),
                new TokenHasher());
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate database,
            RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(database, clients);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> tokenGenerator(JWKSource<SecurityContext> keys, AccessProperties properties) {
        var access = new JwtGenerator(new NimbusJwtEncoder(keys));
        access.setJwtCustomizer(context -> {
            context.getJwsHeader().algorithm(SignatureAlgorithm.RS256).keyId(properties.signingKeyId());
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                var audiences = new ArrayList<>(Collections.singletonList(properties.audience()));
                audiences.add(properties.publicUrl() + "/mcp");
                for (var domain : McpDomain.values()) audiences.add(properties.publicUrl() + "/mcp/" + domain.key());
                context.getClaims().audience(audiences)
                        .claim("client_id", context.getRegisteredClient().getClientId());
            }
        });
        return new DelegatingOAuth2TokenGenerator(access, new PublicClientRefreshTokenGenerator());
    }
}
