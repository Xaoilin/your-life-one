package com.yourlifeone.access.mcp.application;

import com.yourlifeone.access.mcp.domain.BackendCall;
import com.yourlifeone.access.mcp.domain.BackendService;
import com.yourlifeone.access.mcp.domain.McpDomain;
import com.yourlifeone.access.mcp.infrastructure.BackendClient;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinanceTools implements DomainTools {
    private final BackendClient backend;

    @Override
    public McpDomain domain() {
        return McpDomain.FINANCE;
    }

    @Override
    public Object invoke(String tool, Map<String, Object> arguments, Jwt token) {
        return switch (tool) {
            case "finance_get_review" -> {
                var response = backend.call(BackendCall.builder().service(BackendService.FINANCE)
                        .method(HttpMethod.GET).path("/api/finance/v1/review").build(), token);
                yield response instanceof Map<?, ?> body ? body.get("review") : null;
            }
            case "finance_list_loan_profiles" -> profiles(token, null);
            case "finance_get_loan_profile" -> profiles(token, McpArguments.string(arguments, "profileId"));
            case "finance_save_loan_profile" -> backend.call(BackendCall.builder().service(BackendService.FINANCE)
                    .method(HttpMethod.PUT).path("/api/finance/v1/review").requestId(McpArguments.string(arguments, "requestId"))
                    .body(McpArguments.fields("loanProfileChange", McpArguments.fields("operation", "save", "profile", arguments.get("profile")),
                            "expectedUpdatedAt", arguments.get("expectedUpdatedAt"))).build(), token);
            case "finance_remove_loan_profile" -> removeProfile(arguments, token);
            case "finance_save_review" -> backend.call(BackendCall.builder().service(BackendService.FINANCE)
                    .method(HttpMethod.PUT).path("/api/finance/v1/review")
                    .requestId(McpArguments.string(arguments, "requestId"))
                    .body(McpArguments.fields("review", McpArguments.object(arguments, "review"),
                            "expectedUpdatedAt", arguments.get("expectedUpdatedAt"))).build(), token);
            default -> throw new IllegalArgumentException("Unknown Finance tool.");
        };
    }
    private Object profiles(Jwt token, String profileId) {
        Object result = backend.call(BackendCall.builder().service(BackendService.FINANCE).method(HttpMethod.GET)
                .path("/api/finance/v1/review").build(), token);
        var review = result instanceof Map<?, ?> response && response.get("review") instanceof Map<?, ?> value ? value : Map.of();
        var profiles = review.get("loanProfiles") instanceof List<?> values ? values : List.of();
        if (profileId == null) return McpArguments.fields("profiles", profiles, "updatedAt", review.get("updatedAt"));
        var found = profiles.stream().filter(value -> value instanceof Map<?, ?> profile && profileId.equals(profile.get("id")))
                .findFirst().orElse(null);
        return McpArguments.fields("profile", found, "updatedAt", review.get("updatedAt"));
    }

    private Object removeProfile(Map<String, Object> arguments, Jwt token) {
        McpArguments.confirmed(arguments);
        return backend.call(BackendCall.builder().service(BackendService.FINANCE).method(HttpMethod.PUT)
                .path("/api/finance/v1/review").requestId(McpArguments.string(arguments, "requestId"))
                .body(McpArguments.fields("loanProfileChange", McpArguments.fields("operation", "remove",
                                "profileId", arguments.get("profileId"), "confirmed", true),
                        "expectedUpdatedAt", arguments.get("expectedUpdatedAt"))).build(), token);
    }
}
