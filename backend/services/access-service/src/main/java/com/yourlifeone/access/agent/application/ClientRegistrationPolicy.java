package com.yourlifeone.access.agent.application;

import com.yourlifeone.access.agent.config.AgentProperties;
import com.yourlifeone.access.agent.domain.RedirectUriPolicy;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

/**
 * What an AI client may register for itself. Anyone can ask (open registration), so everything is decided here and
 * nothing is taken on trust: the allowed grants and client authentication methods, the redirect addresses, the single
 * scope, mandatory PKCE and consent, and the token lifetimes all come from this policy, whatever the client asks for.
 */
public class ClientRegistrationPolicy {

    private static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";
    private static final String DEFAULT_CLIENT_NAME = "AI client";
    private static final Set<AuthorizationGrantType> ALLOWED_GRANTS =
            Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
    private static final Set<ClientAuthenticationMethod> ALLOWED_METHODS = Set.of(
            ClientAuthenticationMethod.NONE,
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            ClientAuthenticationMethod.CLIENT_SECRET_POST);

    private final AgentProperties properties;
    private final RedirectUriPolicy redirectUris;
    private final Converter<OAuth2ClientRegistration, RegisteredClient> defaults =
            new OAuth2ClientRegistrationRegisteredClientConverter();

    public ClientRegistrationPolicy(AgentProperties properties) {
        this.properties = properties;
        this.redirectUris = new RedirectUriPolicy(properties.extraRedirectSchemes());
    }

    /** Checks a registration request. Unknown scopes are accepted here and replaced by the one real scope later. */
    public Consumer<OAuth2ClientRegistrationAuthenticationContext> validator() {
        return OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                .andThen(this::requireAllowedRedirectUris)
                .andThen(OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR)
                .andThen(OAuth2ClientRegistrationAuthenticationValidator.SIMPLE_SCOPE_VALIDATOR)
                .andThen(this::requireAllowedGrantsAndMethods);
    }

    /** Builds the stored client from the request, then overwrites everything the policy owns. */
    public Converter<OAuth2ClientRegistration, RegisteredClient> converter() {
        return registration -> {
            RegisteredClient requested = defaults.convert(registration);
            return RegisteredClient.from(requested)
                    .clientName(StringUtils.hasText(requested.getClientName()) ? requested.getClientName() : DEFAULT_CLIENT_NAME)
                    .scopes(scopes -> {
                        scopes.clear();
                        scopes.add(properties.scope());
                    })
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(true)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(properties.accessTokenTtl())
                            .refreshTokenTimeToLive(properties.refreshTokenTtl())
                            .authorizationCodeTimeToLive(properties.authorizationCodeTtl())
                            .reuseRefreshTokens(false)
                            .build())
                    .build();
        };
    }

    /** Makes the published metadata list only the grants and client authentication methods this policy accepts. */
    public Consumer<OAuth2AuthorizationServerMetadata.Builder> metadataCustomizer() {
        return metadata -> metadata
                .grantTypes(types -> {
                    types.clear();
                    ALLOWED_GRANTS.forEach(grant -> types.add(grant.getValue()));
                })
                .tokenEndpointAuthenticationMethods(methods -> {
                    methods.clear();
                    ALLOWED_METHODS.forEach(method -> methods.add(method.getValue()));
                });
    }

    private void requireAllowedRedirectUris(OAuth2ClientRegistrationAuthenticationContext context) {
        OAuth2ClientRegistration registration = registrationOf(context);
        List<String> uris = registration.getRedirectUris();
        if (uris == null || uris.isEmpty() || !uris.stream().allMatch(redirectUris::allows)) {
            throw invalid("redirect_uris must be https addresses or loopback http addresses");
        }
    }

    private void requireAllowedGrantsAndMethods(OAuth2ClientRegistrationAuthenticationContext context) {
        OAuth2ClientRegistration registration = registrationOf(context);
        List<String> grants = registration.getGrantTypes();
        if (grants != null && !grants.stream().map(AuthorizationGrantType::new).allMatch(ALLOWED_GRANTS::contains)) {
            throw invalid("grant_types may only be authorization_code and refresh_token");
        }
        String method = registration.getTokenEndpointAuthenticationMethod();
        if (StringUtils.hasText(method) && !ALLOWED_METHODS.contains(new ClientAuthenticationMethod(method))) {
            throw invalid("token_endpoint_auth_method must be none, client_secret_basic or client_secret_post");
        }
    }

    private static OAuth2ClientRegistration registrationOf(OAuth2ClientRegistrationAuthenticationContext context) {
        OAuth2ClientRegistrationAuthenticationToken authentication = context.getAuthentication();
        return authentication.getClientRegistration();
    }

    private static OAuth2AuthenticationException invalid(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(INVALID_CLIENT_METADATA, description, null));
    }
}
