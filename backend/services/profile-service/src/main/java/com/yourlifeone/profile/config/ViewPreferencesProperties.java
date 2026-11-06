package com.yourlifeone.profile.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Product-specific allowed UI choices; arbitrary text and account-record IDs cannot be stored. */
@ConfigurationProperties("app.profile.view-preferences")
public record ViewPreferencesProperties(Map<String, List<String>> allowedValues) {
    public ViewPreferencesProperties {
        var copy = new LinkedHashMap<String, List<String>>();
        if (allowedValues != null) allowedValues.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        allowedValues = Map.copyOf(copy);
    }

    public boolean allows(String key, String value) {
        return key != null && value != null && allowedValues.getOrDefault(key, List.of()).contains(value);
    }
}
