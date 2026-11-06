package com.yourlifeone.access.agent.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Agent connection policy. Domain approval remains separate from the OAuth connection itself. */
@ConfigurationProperties("app.agents")
public record AgentProperties(
        @DefaultValue("mcp") String scope,
        @DefaultValue("10m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("5m") Duration authorizationCodeTtl,
        @DefaultValue({}) List<String> extraRedirectSchemes) {
}
