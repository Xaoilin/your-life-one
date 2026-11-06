package com.yourlifeone.access.mcp.domain;

import java.util.Locale;

/** Fixed semantic domains; tool input can never choose a backend URL or an arbitrary API path. */
public enum McpDomain {
    FINANCE(BackendService.FINANCE, "/api/finance/v1/review/access"),
    EQUITY(BackendService.FINANCE, "/api/finance/v1/equity/access"),
    INVENTORY(BackendService.LIFE, "/api/life/v1/inventory/access"),
    EMPLOYMENT(BackendService.LIFE, "/api/life/v1/jobs/access");

    private final BackendService backend;
    private final String accessPath;

    McpDomain(BackendService backend, String accessPath) {
        this.backend = backend;
        this.accessPath = accessPath;
    }

    public BackendService backend() {
        return backend;
    }

    public String accessPath() {
        return accessPath;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static McpDomain forKey(String key) {
        return valueOf(key.toUpperCase(Locale.ROOT));
    }

    public static McpDomain forTool(String tool) {
        int separator = tool.indexOf('_');
        if (separator < 1) throw new IllegalArgumentException("Unknown tool domain.");
        return valueOf(tool.substring(0, separator).toUpperCase(Locale.ROOT));
    }
}
