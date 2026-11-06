package com.yourlifeone.access.agent.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which redirect addresses an AI client may register. Open client registration means anyone can ask, so the rule is
 * strict: https addresses, loopback http addresses (desktop and command-line tools listening on this computer),
 * and private-use schemes only when the owner listed them. Never a fragment, never embedded credentials.
 */
public final class RedirectUriPolicy {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private final Set<String> extraSchemes;

    public RedirectUriPolicy(Collection<String> extraSchemes) {
        this.extraSchemes = extraSchemes.stream()
                .map(scheme -> scheme.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean allows(String redirectUri) {
        URI uri = parse(redirectUri);
        if (uri == null || uri.getFragment() != null || uri.getUserInfo() != null || uri.getScheme() == null) {
            return false;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        return switch (scheme) {
            case HTTPS -> hasHost(uri);
            case HTTP -> isLoopbackHost(uri.getHost());
            default -> extraSchemes.contains(scheme);
        };
    }

    /** True for localhost, 127.0.0.1 and [::1]. */
    public static boolean isLoopbackHost(String host) {
        return host != null && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    }

    private static boolean hasHost(URI uri) {
        return uri.getHost() != null && !uri.getHost().isBlank();
    }

    private static URI parse(String redirectUri) {
        try {
            return redirectUri == null ? null : new URI(redirectUri);
        } catch (URISyntaxException invalid) {
            return null;
        }
    }
}
