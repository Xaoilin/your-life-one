package com.yourlifeone.access.testing;

import lombok.Builder;
import tools.jackson.databind.JsonNode;

@Builder
public record RecordedBackendRequest(String method, String path, String query, String requestId,
        String userId, String clientId, JsonNode body) {
}
