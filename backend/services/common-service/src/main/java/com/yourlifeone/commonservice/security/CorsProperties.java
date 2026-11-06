package com.yourlifeone.commonservice.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param allowedOrigins browser origins allowed to call the API */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
