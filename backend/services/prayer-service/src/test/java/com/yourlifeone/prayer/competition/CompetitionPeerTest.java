package com.yourlifeone.prayer.competition;

import com.yourlifeone.prayer.competition.config.CompetitionProperties;
import com.yourlifeone.prayer.competition.api.response.CompetitionSummary;
import com.yourlifeone.prayer.competition.infrastructure.CompetitionPeer;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;

class CompetitionPeerTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
    private final LocalDate from = LocalDate.parse("2026-11-01");
    private final LocalDate to = LocalDate.parse("2026-11-30");
    private final CompetitionProperties properties = new CompetitionProperties(UUID.randomUUID(), "a".repeat(40),
            URI.create("https://partner.example.test/api/prayer/v1/competition/summary"), "b".repeat(40));

    @Test
    void readsOnlyTheConfiguredSummaryWithItsDedicatedKey() {
        var builder = RestClient.builder().baseUrl("https://partner.example.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://partner.example.test/api/prayer/v1/competition/summary?from=2026-11-01&to=2026-11-30"))
                .andExpect(header("X-Prayer-Competition-Key", properties.peerKey()))
                .andRespond(withSuccess(body("2026-11-04T12:00:00Z", 8), MediaType.APPLICATION_JSON));
        var peer = new CompetitionPeer(builder.build(), properties, clock);
        assertThat(peer.read(from, to).onTime()).isEqualTo(8);
        server.verify();
    }

    @Test
    void rejectsStaleMalformedOrUnavailableTotals() {
        for (var body : new String[]{body("2026-11-04T11:00:00Z", 8), body("2026-11-04T12:00:00Z", -1), body("2026-11-04T12:00:00Z", 8).replace("\"late\":1,", ""),
                body("2026-11-04T12:00:00Z", 156), body("2026-11-04T12:00:00Z", 8).replace("2026-11-30", "2026-11-29")}) {
            var builder = RestClient.builder().baseUrl("https://partner.example.test");
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(anything())).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThat(new CompetitionPeer(builder.build(), properties, clock).read(from, to)).isNull();
        }
        var builder = RestClient.builder().baseUrl("https://partner.example.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(anything())).andRespond(withServiceUnavailable());
        assertThat(new CompetitionPeer(builder.build(), properties, clock).read(from, to)).isNull();
    }

    @Test
    void acceptsPairedWeightedPointsAndRejectsPartialOrInconsistentPoints() {
        var original = body("2026-11-04T12:00:00Z", 8);
        for (var fields : new String[]{",\"pointsEarned\":74,\"pointsAvailable\":120",
                ",\"pointsEarned\":0,\"pointsAvailable\":120"}) {
            assertThat(readBody(original.replace("}", fields + "}"))).isNotNull();
        }
        for (var fields : new String[]{",\"pointsEarned\":7.4,\"pointsAvailable\":120",
                ",\"pointsEarned\":\"74\",\"pointsAvailable\":120", ",\"pointsEarned\":74", ",\"pointsAvailable\":120",
                ",\"pointsEarned\":-1,\"pointsAvailable\":120",
                ",\"pointsEarned\":121,\"pointsAvailable\":120",
                ",\"pointsEarned\":74,\"pointsAvailable\":119",
                ",\"pointsEarned\":74,\"pointsAvailable\":1861"}) {
            assertThat(readBody(original.replace("}", fields + "}"))).isNull();
        }
    }

    private CompetitionSummary readBody(String body) {
        var builder = RestClient.builder().baseUrl("https://partner.example.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(anything())).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        return new CompetitionPeer(builder.build(), properties, clock).read(from, to);
    }

    private static String body(String asOf, int onTime) {
        return "{\"from\":\"2026-11-01\",\"to\":\"2026-11-30\",\"asOf\":\"" + asOf
                + "\",\"onTime\":" + onTime + ",\"late\":1,\"missed\":1}";
    }
}
