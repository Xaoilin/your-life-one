package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.BackendCall;
import com.yourlifeone.access.mcp.domain.BackendService;
import com.yourlifeone.access.mcp.domain.McpDomain;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryTools implements DomainTools {
    private static final String INVENTORY = "/api/life/v1/inventory";
    private final BackendClient backend;

    @Override
    public McpDomain domain() {
        return McpDomain.INVENTORY;
    }

    @Override
    public Object invoke(String tool, Map<String, Object> arguments, Jwt token) {
        if ("inventory_resolve_project".equals(tool)) return projects(arguments, token);
        var call = BackendCall.builder().service(BackendService.LIFE);
        switch (tool) {
            case "inventory_search" -> {
                var query = McpArguments.query(arguments, "query", "projectCatalogKey", "category", "location", "limit");
                query.putIfAbsent("query", "");
                query.putIfAbsent("limit", 50);
                call.method(HttpMethod.GET).path(INVENTORY + "/search").query(query);
            }
            case "inventory_check" -> {
                var query = McpArguments.query(arguments, "name", "requiredQuantity", "unit");
                query.putIfAbsent("requiredQuantity", 1);
                call.method(HttpMethod.GET).path(INVENTORY + "/check").query(query);
            }
            case "inventory_save_items" -> {
                if (!(arguments.get("items") instanceof List<?> items) || items.isEmpty()) {
                    throw new IllegalArgumentException("At least one item is required.");
                }
                if (items.size() > 1 && !Boolean.TRUE.equals(arguments.get("reviewed"))) {
                    throw new IllegalArgumentException("Review the multi-item candidates with the user before saving them.");
                }
                call.method(HttpMethod.POST).path(INVENTORY + "/items").requestId(requestId(arguments))
                        .body(Map.of("items", items));
            }
            case "inventory_save_need" -> {
                var need = McpArguments.object(arguments, "need");
                var requestId = requestId(arguments);
                var id = need.remove("id");
                // Keep an exact retry on the same new resource when the caller supplied no need id.
                if (id == null) id = requestId;
                call.method(HttpMethod.PUT).path(INVENTORY + "/needs/{id}").variables(Map.of("id", id))
                        .requestId(requestId).body(need);
            }
            case "inventory_complete_need" -> {
                var body = arguments.containsKey("newItemId")
                        ? Map.of("newItemId", arguments.get("newItemId")) : Map.of();
                call.method(HttpMethod.POST).path(INVENTORY + "/needs/{id}/acquire")
                        .variables(Map.of("id", McpArguments.string(arguments, "needId")))
                        .requestId(requestId(arguments)).body(body);
            }
            case "inventory_archive_item" -> {
                McpArguments.confirmed(arguments);
                call.method(HttpMethod.POST).path(INVENTORY + "/items/{id}/archive")
                        .variables(Map.of("id", McpArguments.string(arguments, "itemId"))).requestId(requestId(arguments));
            }
            default -> throw new IllegalArgumentException("Unknown Inventory tool.");
        }
        return backend.call(call.build(), token);
    }

    private Object projects(Map<String, Object> arguments, Jwt token) {
        var response = backend.call(BackendCall.builder().service(BackendService.KNOWLEDGE).method(HttpMethod.GET)
                .path("/api/knowledge/v1/projects/resolve").query(McpArguments.query(arguments, "query")).build(), token);
        if (response instanceof Map<?, ?> body && body.get("projects") instanceof List<?> projects) return projects;
        return List.of();
    }

    private String requestId(Map<String, Object> arguments) {
        return arguments.containsKey("requestId") ? McpArguments.string(arguments, "requestId") : UUID.randomUUID().toString();
    }
}
