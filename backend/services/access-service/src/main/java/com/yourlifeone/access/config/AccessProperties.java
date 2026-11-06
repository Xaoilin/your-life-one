package com.yourlifeone.access.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/** Independent product origins and signing keys; no runtime credential is exposed to the browser. */
@ConfigurationProperties("app.access")
@Builder
public record AccessProperties(
        URI publicUrl,
        URI browserUrl,
        List<String> allowedOrigins,
        String signingKeys,
        String signingKeyId,
        @DefaultValue("authenticated") String audience,
        @DefaultValue("10m") Duration accessTokenLifetime) {
    public AccessProperties {
        if (publicUrl == null || browserUrl == null || !publicUrl.isAbsolute() || !browserUrl.isAbsolute()) {
            throw new IllegalArgumentException("Explicit absolute access-service and browser URLs are required.");
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("An explicit browser origin allowlist is required.");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
        if (!StringUtils.hasText(signingKeys) || !StringUtils.hasText(signingKeyId)) {
            throw new IllegalArgumentException("A persistent signing key set and active key id are required.");
        }
        if (!StringUtils.hasText(audience) || accessTokenLifetime == null
                || accessTokenLifetime.isNegative() || accessTokenLifetime.isZero()
                || accessTokenLifetime.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("A nonempty audience and token lifetime of at most one hour are required.");
        }
    }
    @Override
    public String toString() {
        return "AccessProperties[publicUrl=" + publicUrl + ", browserUrl=" + browserUrl + ", signingKeys=REDACTED]";
    }
}
