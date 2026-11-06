package com.yourlifeone.access.agent.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.mcp.application.McpRequestAuthorization;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/** Agent transport uses bearer tokens only. A browser session or browser access token is insufficient. */
@Configuration(proxyBeanMethods = false)
public class McpResourceSecurityConfiguration {
    private static final String METADATA = "/.well-known/oauth-protected-resource";

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> keys, AccessProperties properties) {
        var decoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(keys);
        var audience = new JwtClaimValidator<List<String>>("aud",
                values -> values != null && values.contains(properties.publicUrl() + "/mcp"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.publicUrl().toString()), audience));
        return decoder;
    }

    @Bean
    @Order(2)
    SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, JwtDecoder decoder,
            AccessProperties access, AgentProperties agents) throws Exception {
        var resource = access.publicUrl() + "/mcp";
        var challenge = new BearerTokenAuthenticationEntryPoint();
        challenge.setResourceMetadataParameterResolver(request -> {
            var domain = request.getHeader(McpRequestAuthorization.DOMAIN_HEADER);
            return domain == null ? access.publicUrl() + METADATA
                    : access.publicUrl() + "/mcp-metadata/" + McpDomain.forKey(domain).key();
        });
        http.securityMatcher("/mcp", "/mcp/**", METADATA, METADATA + "/**")
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(METADATA, METADATA + "/**").permitAll()
                        .anyRequest().hasAuthority("SCOPE_" + agents.scope()))
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.decoder(decoder))
                        .authenticationEntryPoint(challenge)
                        .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(builder -> builder
                                .resource(resource).authorizationServer(access.publicUrl().toString())
                                .scope(agents.scope()).tlsClientCertificateBoundAccessTokens(false))))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
