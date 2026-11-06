package com.yourlifeone.access.config;

import com.yourlifeone.access.identity.application.AccountSignInHandler;
import com.yourlifeone.access.identity.application.AccountSessionLogout;
import com.yourlifeone.access.identity.application.ExistingAccountOidcUserService;
import com.yourlifeone.access.identity.infrastructure.DiscardingAuthorizedClientRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateful browser sign-in; application API access tokens remain short-lived and held in memory. */
@Configuration(proxyBeanMethods = false)
public class BrowserSecurityConfiguration {
    @Bean
    @Order(3)
    SecurityFilterChain browserSecurityFilterChain(HttpSecurity http, AccessProperties properties,
            ClientRegistrationRepository registrations, ExistingAccountOidcUserService users,
            AccountSignInHandler signedIn, AccountSessionLogout logout) throws Exception {
        var authorization = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        authorization.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(parameters ->
                parameters.put("include_granted_scopes", "false")));
        http.cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/health/database", "/session", "/csrf", "/signin",
                                "/.well-known/jwks.json", "/mcp-metadata/*", "/oauth2/authorization/**", "/login/oauth2/code/**",
                                "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .loginPage("/signin")
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorization))
                        .authorizedClientRepository(new DiscardingAuthorizedClientRepository())
                        .userInfoEndpoint(endpoint -> endpoint.oidcUserService(users))
                        .successHandler(signedIn)
                        .failureHandler((request, response, failure) -> {
                            response.setStatus(303);
                            response.setHeader("Location", properties.browserUrl() + "?auth=failed");
                        }))
                .logout(configurer -> configurer.logoutUrl("/signout").addLogoutHandler(logout)
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AccessProperties properties) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Authorization",
                "Mcp-Protocol-Version", "Mcp-Session-Id", "Mcp-Method", "Mcp-Name"));
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        // Agent clients use explicit bearer/PKCE credentials, never the person's browser cookies.
        var agents = new CorsConfiguration();
        agents.setAllowedOrigins(List.of("*"));
        agents.setAllowCredentials(false);
        agents.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        agents.setAllowedHeaders(cors.getAllowedHeaders());
        agents.setExposedHeaders(List.of("WWW-Authenticate", "Mcp-Session-Id", "Mcp-Protocol-Version"));
        agents.setMaxAge(3600L);
        for (var path : List.of("/mcp", "/mcp/**", "/mcp-metadata/*", "/.well-known/**",
                "/oauth2/register", "/oauth2/token", "/oauth2/jwks")) {
            source.registerCorsConfiguration(path, agents);
        }
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
