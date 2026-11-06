package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.McpDomain;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.jwt.Jwt;

/** Approval memoization lasts only for this HTTP request. Every backend data call also rechecks approval. */
public final class McpRequestAuthorization {
    public static final String DOMAIN_HEADER = "X-Product-Mcp-Domain";
    private final McpDomain endpointDomain;
    private final Jwt token;
    private final BackendClient backend;
    private final Map<McpDomain, Boolean> approvals = new ConcurrentHashMap<>();

    public McpRequestAuthorization(Jwt token, BackendClient backend, McpDomain endpointDomain) {
        this.endpointDomain = endpointDomain;
        this.token = token;
        this.backend = backend;
    }

    public Jwt token() {
        return token;
    }

    public boolean canAccess(McpDomain domain) {
        if (endpointDomain != null && domain != endpointDomain) return false;
        return approvals.computeIfAbsent(domain, key -> backend.canAccess(key, token));
    }

    @Override
    public String toString() {
        return "McpRequestAuthorization[verified]";
    }
}
