package com.yourlifeone.access.mcp.domain;

import java.util.Map;
import lombok.Builder;
import org.springframework.http.HttpMethod;

@Builder
public record BackendCall(BackendService service, HttpMethod method, String path,
        Map<String, Object> variables, Map<String, Object> query, Object body, String requestId) {
    public BackendCall {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        query = query == null ? Map.of() : query;
    }
}
