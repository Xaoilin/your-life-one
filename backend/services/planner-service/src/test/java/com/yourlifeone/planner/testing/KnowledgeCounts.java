package com.yourlifeone.planner.testing;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The knowledge service's counts ({@code GET /api/knowledge/v1/progress}) as the planner reads them: zero
 * unless a test set them for the user.
 */
public final class KnowledgeCounts {
    static final String PATH = "/api/knowledge/v1/progress";

    private final JsonMapper json;
    private final Map<UUID, Integer> entries = new ConcurrentHashMap<>();
    private final Set<UUID> unavailable = ConcurrentHashMap.newKeySet();

    KnowledgeCounts(JsonMapper json) {
        this.json = json;
    }

    public void entries(UUID user, int count) {
        entries.put(user, count);
    }

    /** The knowledge service stops answering this user's requests. */
    public void unavailableFor(UUID user) {
        unavailable.add(user);
    }

    ClientHttpResponse respond(ClientHttpRequest request) throws IOException {
        var user = Callers.of(request);
        if (user.isPresent() && unavailable.contains(user.get())) return withServerError().createResponse(request);
        var body = json.createObjectNode()
                .put("entries", user.map(caller -> entries.getOrDefault(caller, 0)).orElse(0))
                .put("topics", 0).put("haramMastered", 0).put("halalConsistent", 0).put("lifestyleItems", 0);
        return withSuccess(body.toString(), MediaType.APPLICATION_JSON).createResponse(request);
    }
}
