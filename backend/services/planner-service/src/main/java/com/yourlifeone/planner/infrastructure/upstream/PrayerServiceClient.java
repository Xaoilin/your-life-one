package com.yourlifeone.planner.infrastructure.upstream;

import com.yourlifeone.planner.config.UpstreamProperties;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.infrastructure.upstream.dto.RewardedOutcomesDto;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads from the prayer service as the signed-in user, forwarding their access token. */
@Component
@RequiredArgsConstructor
public class PrayerServiceClient {
    static final String REWARDED_OUTCOMES = "/api/prayer/v1/outcomes/rewarded";
    private static final String BEARER = "Bearer ";

    private final RestClient upstreamRestClient;
    private final UpstreamProperties properties;
    private final UpstreamMapper mapper;

    /**
     * Every prayer the prayer service recorded as prayed (its one-time reward receipt is set).
     *
     * @throws UpstreamUnavailableException when the prayer service does not answer with them
     */
    public List<PrayerOutcome> rewardedOutcomes(String accessToken) {
        try {
            var body = upstreamRestClient.get()
                    .uri(properties.prayerUrl() + REWARDED_OUTCOMES)
                    .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                    .retrieve()
                    .body(RewardedOutcomesDto.class);
            if (body == null || body.outcomes() == null) throw new RestClientException("empty answer");
            return mapper.toDomain(body.outcomes().stream().filter(Objects::nonNull).toList());
        } catch (RestClientException exception) {
            throw new UpstreamUnavailableException("Prayer service unavailable: " + exception.getMessage(), exception);
        }
    }
}
