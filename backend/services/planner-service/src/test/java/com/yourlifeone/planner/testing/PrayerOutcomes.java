package com.yourlifeone.planner.testing;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The prayer service's rewarded outcomes ({@code GET /api/prayer/v1/outcomes/rewarded}) as the planner reads
 * them, with the rows a test needs, per user.
 */
public final class PrayerOutcomes {
    static final String PATH = "/api/prayer/v1/outcomes/rewarded";

    private final JsonMapper json;
    private final Map<UUID, List<ObjectNode>> rewarded = new ConcurrentHashMap<>();
    private final Set<UUID> unavailable = ConcurrentHashMap.newKeySet();

    PrayerOutcomes(JsonMapper json) {
        this.json = json;
    }

    public void prayed(UUID user, LocalDate date, String prayer, Instant recordedAt, String taskId) {
        rewarded.computeIfAbsent(user, key -> new CopyOnWriteArrayList<>()).add(json.createObjectNode()
                .put("date", date.toString()).put("prayer", prayer).put("recordedAt", recordedAt.toString())
                .put("taskId", taskId));
    }

    /** Undo: the prayer service deletes the outcome with its reward receipt. */
    public void undone(UUID user, LocalDate date, String prayer) {
        rewarded.getOrDefault(user, List.of()).removeIf(outcome -> outcome.get("date").asString().equals(date.toString())
                && outcome.get("prayer").asString().equals(prayer));
    }

    /** The prayer service stops answering this user's requests. */
    public void unavailableFor(UUID user) {
        unavailable.add(user);
    }

    ClientHttpResponse respond(ClientHttpRequest request) throws IOException {
        var user = Callers.of(request);
        if (user.isPresent() && unavailable.contains(user.get())) return withServiceUnavailable().createResponse(request);
        var body = json.createObjectNode();
        var outcomes = body.putArray("outcomes");
        user.map(caller -> rewarded.getOrDefault(caller, List.of())).orElse(List.of()).forEach(outcomes::add);
        return withSuccess(body.toString(), MediaType.APPLICATION_JSON).createResponse(request);
    }
}
