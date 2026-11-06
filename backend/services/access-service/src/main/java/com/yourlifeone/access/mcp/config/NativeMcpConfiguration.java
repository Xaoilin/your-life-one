package com.yourlifeone.access.mcp.config;

import com.yourlifeone.access.mcp.application.McpRequestAuthorization;
import com.yourlifeone.access.mcp.application.McpToolCatalog;
import com.yourlifeone.access.mcp.application.McpToolDispatcher;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import com.yourlifeone.access.mcp.domain.McpDomain;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.customizer.McpStatelessSyncServerCustomizer;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

/** Standard stateless MCP transport with explicit per-request identity propagation and tool validation. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpBackendProperties.class)
public class NativeMcpConfiguration {
    @Bean
    WebMvcStatelessServerTransport webMvcStatelessServerTransport(JsonMapper json, BackendClient backend) {
        return WebMvcStatelessServerTransport.builder().jsonMapper(new JacksonMcpJsonMapper(json))
                .messageEndpoint("/mcp").contextExtractor(request -> {
                    if (!(request.servletRequest().getUserPrincipal() instanceof JwtAuthenticationToken authentication)
                            || authentication.getToken().getClaimAsString("client_id") == null) {
                        throw new AccessDeniedException("A verified agent token is required.");
                    }
                    var domain = request.servletRequest().getHeader(McpRequestAuthorization.DOMAIN_HEADER);
                    var endpoint = domain == null ? null : McpDomain.forKey(domain);
                    return McpTransportContext.create(Map.of(McpToolDispatcher.AUTHORIZATION,
                            new McpRequestAuthorization(authentication.getToken(), backend, endpoint)));
                }).build();
    }

    @Bean
    List<SyncToolSpecification> productTools(McpToolCatalog catalog, McpToolDispatcher dispatcher) {
        return catalog.tools().stream().map(tool -> SyncToolSpecification.builder()
                .tool(tool).callHandler(dispatcher::call).build()).toList();
    }

    @Bean
    McpStatelessSyncServerCustomizer productToolAuthorization(McpToolDispatcher dispatcher) {
        return server -> server.validateToolInputs(true).addToolFilter(dispatcher::visible);
    }
}
