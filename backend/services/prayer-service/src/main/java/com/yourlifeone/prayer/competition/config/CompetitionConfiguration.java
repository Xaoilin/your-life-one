package com.yourlifeone.prayer.competition.config;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CompetitionProperties.class)
public class CompetitionConfiguration {
    public static final String SUMMARY_PATH = "/api/prayer/v1/competition/summary";

    /** A dedicated read-only capability; other prayer routes still require this product's JWT. */
    @Bean
    @Order(0)
    SecurityFilterChain competitionSummarySecurity(HttpSecurity http, CompetitionProperties properties) throws Exception {
        return http.securityMatcher(SUMMARY_PATH)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().access((authentication, context) -> {
                    var supplied = context.getRequest().getHeader("X-Prayer-Competition-Key");
                    var allowed = properties.canShare() && "GET".equals(context.getRequest().getMethod())
                            && supplied != null && MessageDigest.isEqual(
                                    properties.readKey().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
                    return new AuthorizationDecision(allowed);
                })).build();
    }

    @Bean
    RestClient competitionRestClient(RestClient.Builder builder, CompetitionProperties properties) {
        var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofSeconds(3));
        var origin = properties.peerUrl() == null ? "https://competition.invalid"
                : properties.peerUrl().resolve("/").toString();
        return builder.baseUrl(origin).requestFactory(factory).build();
    }
}
