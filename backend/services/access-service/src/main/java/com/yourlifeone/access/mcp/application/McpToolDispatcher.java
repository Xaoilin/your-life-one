package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.BackendCallFailure;
import com.yourlifeone.access.mcp.domain.McpDomain;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Invokes only named domain tools and preserves the existing result envelope. */
@Component
public class McpToolDispatcher {
    public static final String AUTHORIZATION = "product-authorization";
    private final Map<McpDomain, DomainTools> domains = new EnumMap<>(McpDomain.class);
    private final ObjectMapper json;

    public McpToolDispatcher(List<DomainTools> handlers, ObjectMapper json) {
        this.json = json;
        for (DomainTools handler : handlers) {
            if (domains.put(handler.domain(), handler) != null) throw new IllegalStateException("Duplicate domain handler.");
        }
    }

    public boolean visible(McpTransportContext context, Tool tool) {
        return authorization(context).canAccess(McpDomain.forTool(tool.name()));
    }

    public CallToolResult call(McpTransportContext context, CallToolRequest request) {
        try {
            var authorization = authorization(context);
            var domain = McpDomain.forTool(request.name());
            if (!authorization.canAccess(domain)) {
                throw new BackendCallFailure("agent_not_approved", 403,
                        "This agent is not approved for this part of the product.");
            }
            var result = domains.get(domain).invoke(request.name(), request.arguments(), authorization.token());
            return CallToolResult.builder().addTextContent(json.writeValueAsString(result))
                    .structuredContent(McpArguments.fields("result", result)).isError(false).build();
        } catch (BackendCallFailure | IllegalArgumentException failure) {
            return CallToolResult.builder().addTextContent(failure.getMessage()).isError(true).build();
        }
    }

    private McpRequestAuthorization authorization(McpTransportContext context) {
        if (!(context.get(AUTHORIZATION) instanceof McpRequestAuthorization authorization)) {
            throw new IllegalArgumentException("A verified product agent token is required.");
        }
        return authorization;
    }
}
