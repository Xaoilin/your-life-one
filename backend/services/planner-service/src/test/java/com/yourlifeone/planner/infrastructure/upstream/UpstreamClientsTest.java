package com.yourlifeone.planner.infrastructure.upstream;

import com.yourlifeone.planner.config.UpstreamProperties;
import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Consumer side of the prayer and knowledge contracts: the planner reads the providers' own fixtures
 * ({@code contracts/prayer-service/rewarded-outcomes.json}, {@code contracts/knowledge-service/progress.json})
 * as the user, forwarding their token, and treats any failure as the service being unavailable.
 */
class UpstreamClientsTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UpstreamProperties PROPERTIES = new UpstreamProperties("https://prayer.example",
            "https://knowledge.example", Duration.ofSeconds(1), Duration.ofSeconds(1));
    private static final String TOKEN = "user-access-token";

    private MockRestServiceServer server;
    private PrayerServiceClient prayer;
    private KnowledgeServiceClient knowledge;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build();
        var mapper = new UpstreamMapperImpl();
        prayer = new PrayerServiceClient(client, PROPERTIES, mapper);
        knowledge = new KnowledgeServiceClient(client, PROPERTIES, mapper);
    }

    private static String fixtureBody(String fixture) {
        var root = Path.of("").toAbsolutePath();
        while (!Files.exists(root.resolve("mvnw"))) root = root.getParent();
        try {
            return JSON.readTree(Files.readString(root.resolve("contracts").resolve(fixture))).get("body").toString();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    void readsTheRewardedOutcomesAsTheUser() {
        server.expect(requestTo("https://prayer.example/api/prayer/v1/outcomes/rewarded"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(fixtureBody("prayer-service/rewarded-outcomes.json"), MediaType.APPLICATION_JSON));

        var outcomes = prayer.rewardedOutcomes(TOKEN);

        assertEquals(new PrayerOutcome(LocalDate.parse("2026-09-24"), "Isha", Instant.parse("2026-09-24T20:30:00Z"),
                null), outcomes.getFirst());
        assertEquals("task-dhuhr", outcomes.get(1).taskId());
        server.verify();
    }

    @Test
    void readsTheKnowledgeCountsAsTheUser() {
        server.expect(requestTo("https://knowledge.example/api/knowledge/v1/progress"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(fixtureBody("knowledge-service/progress.json"), MediaType.APPLICATION_JSON));

        assertEquals(new KnowledgeProgress(1, 1, 0, 0, 0), knowledge.progress(TOKEN));
    }

    @Test
    void ignoresFieldsItDoesNotKnow() {
        server.expect(requestTo("https://knowledge.example/api/knowledge/v1/progress")).andRespond(withSuccess("""
                {"entries":3,"topics":2,"haramMastered":1,"halalConsistent":0,"lifestyleItems":4,"projects":9}""",
                MediaType.APPLICATION_JSON));

        assertEquals(new KnowledgeProgress(3, 2, 1, 0, 4), knowledge.progress(TOKEN));
    }

    @Test
    void anErrorOrAnUnreadableAnswerMeansUnavailable() {
        server.expect(requestTo("https://prayer.example/api/prayer/v1/outcomes/rewarded")).andRespond(withServerError());
        assertThrows(UpstreamUnavailableException.class, () -> prayer.rewardedOutcomes(TOKEN));

        server.reset();
        server.expect(requestTo("https://knowledge.example/api/knowledge/v1/progress"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThrows(UpstreamUnavailableException.class, () -> knowledge.progress(TOKEN));
    }
}
