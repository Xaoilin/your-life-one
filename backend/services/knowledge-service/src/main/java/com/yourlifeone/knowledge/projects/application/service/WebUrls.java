package com.yourlifeone.knowledge.projects.application.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/** Web addresses a project may show: absolute http or https URLs with a host. */
public final class WebUrls {
    private static final Set<String> WEB_SCHEMES = Set.of("http", "https");

    private WebUrls() {
    }

    /** The trimmed address when it is a web URL; null otherwise. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        var candidate = value.trim();
        try {
            var uri = new URI(candidate);
            var scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return WEB_SCHEMES.contains(scheme) && uri.getHost() != null ? candidate : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }
}
