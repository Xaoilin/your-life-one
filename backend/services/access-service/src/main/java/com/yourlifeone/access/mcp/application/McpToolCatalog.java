package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.McpDomain;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Discovery comes from the frozen legacy tool contracts, not regenerated Java method signatures. */
@Component
public class McpToolCatalog {
    private final List<Tool> tools;

    public McpToolCatalog(JsonMapper json) throws IOException {
        var mapper = new JacksonMcpJsonMapper(json);
        var loaded = new ArrayList<Tool>();
        var names = new HashSet<String>();
        for (McpDomain domain : McpDomain.values()) {
            try (var input = new ClassPathResource("mcp/" + domain.key() + "-tools.json").getInputStream()) {
                List<Tool> definitions = mapper.readValue(input.readAllBytes(), new TypeRef<>() { });
                for (Tool tool : definitions) {
                    if (McpDomain.forTool(tool.name()) != domain || !names.add(tool.name())) {
                        throw new IllegalStateException("Invalid or duplicate domain tool contract.");
                    }
                    loaded.add(tool);
                }
            }
        }
        tools = List.copyOf(loaded);
    }

    public List<Tool> tools() {
        return tools;
    }
}
