package com.yourlifeone.access.agent.api.controller;

import com.yourlifeone.access.agent.api.response.ProtectedResourceMetadata;
import com.yourlifeone.access.agent.config.AgentProperties;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Distinct MCP connection addresses retain their own resource identity and visible tool domain. */
@RestController
@RequiredArgsConstructor
public class McpMetadataController {
    private final AccessProperties access;
    private final AgentProperties agents;

    @GetMapping("/mcp-metadata/{domain}")
    public ProtectedResourceMetadata metadata(@PathVariable String domain) {
        McpDomain selected;
        try {
            selected = McpDomain.forKey(domain);
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ProtectedResourceMetadata.builder().resource(URI.create(access.publicUrl() + "/mcp/" + selected.key()))
                .authorizationServers(List.of(access.publicUrl())).scopesSupported(List.of(agents.scope()))
                .bearerMethodsSupported(List.of("header")).build();
    }
}
