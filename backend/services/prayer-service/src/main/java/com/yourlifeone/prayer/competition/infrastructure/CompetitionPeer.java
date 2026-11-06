package com.yourlifeone.prayer.competition.infrastructure;

import com.yourlifeone.prayer.competition.api.response.CompetitionSummary;
import com.yourlifeone.prayer.competition.config.CompetitionProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Only the operator-selected summary endpoint is reachable; never accepts a URL or account ID from a caller. */
@Component
@Slf4j
public class CompetitionPeer {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final RestClient client;
    private final CompetitionProperties properties;
    private final Clock clock;

    public CompetitionPeer(@Qualifier("competitionRestClient") RestClient client,
            CompetitionProperties properties, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    public CompetitionSummary read(LocalDate from, LocalDate to) {
        try {
            var payload = client.get().uri(builder -> builder.path(properties.peerUrl().getPath())
                            .queryParam("from", from).queryParam("to", to).build())
                    .header("X-Prayer-Competition-Key", properties.peerKey())
                    .retrieve().body(JsonNode.class);
            var summary = payload == null || !integerPoints(payload) ? null
                    : JSON.treeToValue(payload, CompetitionSummary.class);
            if (valid(summary, from, to)) return summary;
            log.warn("Prayer competition partner returned invalid or stale totals");
            return null;
        } catch (RestClientException | JacksonException exception) {
            // No peer URL, response body or credential enters diagnostics.
            log.warn("Prayer competition partner unavailable ({})", exception.getClass().getSimpleName());
            return null;
        }
    }

    private boolean integerPoints(JsonNode payload) {
        for (var field : new String[]{"pointsEarned", "pointsAvailable"}) {
            var value = payload.get(field);
            if (value != null && !value.isNull() && (!value.isIntegralNumber() || !value.canConvertToInt())) return false;
        }
        var completion = payload.get("allPrayed");
        return completion == null || completion.isNull() || completion.isBoolean();
    }

    private boolean valid(CompetitionSummary summary, LocalDate from, LocalDate to) {
        return summary != null && from.equals(summary.from()) && to.equals(summary.to())
                && summary.asOf() != null
                && Duration.between(summary.asOf(), clock.instant()).abs().compareTo(Duration.ofMinutes(5)) <= 0
                && summary.onTime() != null && summary.late() != null && summary.missed() != null
                && summary.onTime() >= 0 && summary.late() >= 0 && summary.missed() >= 0
                && (long) summary.onTime() + summary.late() + summary.missed() <= 155
                && validPoints(summary);
    }

    private boolean validPoints(CompetitionSummary summary) {
        // Rolling deployment: an old peer may supply neither field, but never just one.
        if (summary.pointsEarned() == null && summary.pointsAvailable() == null) return true;
        if (summary.pointsEarned() == null || summary.pointsAvailable() == null) return false;
        long classified = (long) summary.onTime() + summary.late() + summary.missed();
        return summary.pointsEarned() >= 0 && summary.pointsAvailable() >= 0
                && summary.pointsAvailable() <= 1860
                && summary.pointsEarned() <= summary.pointsAvailable()
                && summary.pointsAvailable() == 12 * classified;
    }
}
