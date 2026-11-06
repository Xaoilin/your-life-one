package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.BackendCall;
import com.yourlifeone.access.mcp.domain.BackendService;
import com.yourlifeone.access.mcp.domain.McpDomain;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmploymentTools implements DomainTools {
    private static final String APPLICATIONS = "/api/life/v1/jobs/applications";
    private final BackendClient backend;

    @Override
    public McpDomain domain() {
        return McpDomain.EMPLOYMENT;
    }

    @Override
    public Object invoke(String tool, Map<String, Object> arguments, Jwt token) {
        var call = BackendCall.builder().service(BackendService.LIFE);
        switch (tool) {
            case "employment_list_applications" -> {
                var query = McpArguments.query(arguments, "query", "status", "workType", "remoteStatus", "limit", "offset");
                query.putIfAbsent("query", "");
                query.putIfAbsent("limit", 50);
                query.putIfAbsent("offset", 0);
                call.method(HttpMethod.GET).path(APPLICATIONS).query(query);
            }
            case "employment_get_application" -> call.method(HttpMethod.GET).path(APPLICATIONS + "/{id}")
                    .variables(Map.of("id", McpArguments.string(arguments, "applicationId")));
            case "employment_add_application" -> call.method(HttpMethod.POST).path(APPLICATIONS)
                    .requestId(McpArguments.string(arguments, "requestId"))
                    .body(McpArguments.object(arguments, "application"));
            case "employment_update_application" -> call.method(HttpMethod.PATCH).path(APPLICATIONS + "/{id}")
                    .variables(Map.of("id", McpArguments.string(arguments, "applicationId")))
                    .requestId(McpArguments.string(arguments, "requestId"))
                    .body(patch(McpArguments.object(arguments, "patch")));
            case "employment_add_history" -> call.method(HttpMethod.POST).path(APPLICATIONS + "/{id}/history")
                    .variables(Map.of("id", McpArguments.string(arguments, "applicationId")))
                    .requestId(McpArguments.string(arguments, "requestId"))
                    .body(McpArguments.object(arguments, "history"));
            case "employment_remove_application" -> {
                McpArguments.confirmed(arguments);
                call.method(HttpMethod.DELETE).path(APPLICATIONS + "/{id}")
                        .variables(Map.of("id", McpArguments.string(arguments, "applicationId")))
                        .requestId(McpArguments.string(arguments, "requestId")).query(Map.of("confirm", true));
            }
            default -> throw new IllegalArgumentException("Unknown Employment tool.");
        }
        return backend.call(call.build(), token);
    }

    private Map<String, Object> patch(Map<String, Object> patch) {
        var fields = new LinkedHashMap<String, Object>();
        var clear = new ArrayList<String>();
        patch.forEach((key, value) -> {
            if (value == null) clear.add(key);
            else fields.put(key, value);
        });
        if (!clear.isEmpty()) fields.put("clear", clear);
        return fields;
    }
}
