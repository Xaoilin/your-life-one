package com.yourlifeone.access.mcp.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.mcp.backends")
public record McpBackendProperties(
        @DefaultValue("http://life:8080") URI life,
        @DefaultValue("http://finance:8080") URI finance,
        @DefaultValue("http://knowledge:8080") URI knowledge,
        @DefaultValue("12s") Duration timeout) {
}
