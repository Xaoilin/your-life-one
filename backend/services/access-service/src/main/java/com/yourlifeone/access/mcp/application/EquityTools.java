package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.BackendCall;
import com.yourlifeone.access.mcp.domain.BackendService;
import com.yourlifeone.access.mcp.domain.McpDomain;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EquityTools implements DomainTools {
    private static final String POSITIONS = "/api/finance/v1/equity/positions";
    private final BackendClient backend;

    @Override
    public McpDomain domain() {
        return McpDomain.EQUITY;
    }

    @Override
    public Object invoke(String tool, Map<String, Object> arguments, Jwt token) {
        return switch (tool) {
            case "equity_list_positions" -> list(arguments, token);
            case "equity_get_position" -> McpArguments.fields("position", backend.call(BackendCall.builder()
                    .service(BackendService.FINANCE).method(HttpMethod.GET).path(POSITIONS + "/{id}")
                    .variables(Map.of("id", McpArguments.string(arguments, "positionId"))).build(), token));
            case "equity_add_position", "equity_update_position" -> save(tool, arguments, token);
            case "equity_remove_position" -> remove(arguments, token);
            default -> throw new IllegalArgumentException("Unknown Equity tool.");
        };
    }

    @SuppressWarnings("unchecked")
    private Object list(Map<String, Object> arguments, Jwt token) {
        var query = McpArguments.query(arguments, "query", "limit", "offset");
        query.putIfAbsent("query", ""); query.putIfAbsent("limit", 50); query.putIfAbsent("offset", 0);
        var response = backend.call(BackendCall.builder().service(BackendService.FINANCE).method(HttpMethod.GET)
                .path(POSITIONS).query(query).build(), token);
        var result = new LinkedHashMap<>((Map<String, Object>) response);
        result.put("limit", query.get("limit")); result.put("offset", query.get("offset"));
        return result;
    }

    private Object save(String tool, Map<String, Object> arguments, Jwt token) {
        boolean adding = "equity_add_position".equals(tool);
        var position = McpArguments.object(arguments, "position");
        var requestId = McpArguments.string(arguments, "requestId");
        var id = adding ? position.remove("id") : McpArguments.string(arguments, "positionId");
        if (id == null) id = requestId;
        var saved = backend.call(BackendCall.builder().service(BackendService.FINANCE).method(HttpMethod.PUT)
                .path(POSITIONS + "/{id}").variables(Map.of("id", id)).requestId(requestId)
                .body(McpArguments.fields("position", position,
                        "expectedUpdatedAt", adding ? null : arguments.get("expectedUpdatedAt"))).build(), token);
        return McpArguments.fields("positionId", ((Map<?, ?>) saved).get("id"), "position", saved);
    }

    private Object remove(Map<String, Object> arguments, Jwt token) {
        McpArguments.confirmed(arguments);
        var removed = backend.call(BackendCall.builder().service(BackendService.FINANCE).method(HttpMethod.DELETE)
                .path(POSITIONS + "/{id}").variables(Map.of("id", McpArguments.string(arguments, "positionId")))
                .requestId(McpArguments.string(arguments, "requestId"))
                .query(McpArguments.fields("expectedUpdatedAt", arguments.get("expectedUpdatedAt"))).build(), token);
        return McpArguments.fields("positionId", ((Map<?, ?>) removed).get("id"), "position", null);
    }
}
