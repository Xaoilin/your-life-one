package com.yourlifeone.profile.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integrations the app offers ({@code app.profile.integrations.*}).
 *
 * @param providers providers a user may record a connection for, e.g. {@code google}
 */
@ConfigurationProperties("app.profile.integrations")
public record IntegrationProperties(List<String> providers) {
    public IntegrationProperties {
        providers = providers == null ? List.of() : List.copyOf(providers);
    }
}
