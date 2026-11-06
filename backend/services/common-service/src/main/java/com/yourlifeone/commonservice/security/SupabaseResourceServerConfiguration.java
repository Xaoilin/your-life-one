package com.yourlifeone.commonservice.security;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless API security shared by services that hold user data: health checks are public and
 * every other route needs a signed access token from this product's configured identity provider.
 * Import it from a service configuration; it is deliberately not auto-configured.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({IdentityProviderProperties.class, CorsProperties.class})
public class SupabaseResourceServerConfiguration {
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type", IdempotencyKeys.HEADER);
    /** Retry-After is readable so the app waits as long as a 429 or 503 asks. */
    private static final List<String> EXPOSED_HEADERS = List.of(IdempotencyKeys.REPLAYED_HEADER, HttpHeaders.RETRY_AFTER);
    private static final long PREFLIGHT_CACHE_SECONDS = 3600;

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // A streamed response (server-sent events) finishes on an async dispatch; its
                        // request was already authorized, and the bearer token is not read again then.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/*/health", "/api/*/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(ALLOWED_METHODS);
        cors.setAllowedHeaders(ALLOWED_HEADERS);
        cors.setExposedHeaders(EXPOSED_HEADERS);
        cors.setMaxAge(PREFLIGHT_CACHE_SECONDS);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder identityJwtDecoder(IdentityProviderProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
                .jwsAlgorithm(properties.signatureAlgorithm())
                .build();
        decoder.setJwtValidator(SupabaseTokenValidators.forIssuer(properties.issuer(), properties.audience()));
        return decoder;
    }
}
