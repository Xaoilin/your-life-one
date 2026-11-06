package com.yourlifeone.profile.domain;

import java.util.Set;

/** The connection states an integration can be in. */
public final class IntegrationStatus {
    public static final String CONNECTED = "connected";
    public static final String DISCONNECTED = "disconnected";
    public static final String ERROR = "error";
    public static final Set<String> ALL = Set.of(CONNECTED, DISCONNECTED, ERROR);

    private IntegrationStatus() {
    }
}
