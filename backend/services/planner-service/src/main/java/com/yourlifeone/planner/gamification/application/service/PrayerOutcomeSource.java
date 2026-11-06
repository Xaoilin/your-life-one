package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.infrastructure.upstream.PrayerServiceClient;
import com.yourlifeone.planner.infrastructure.upstream.UpstreamUnavailableException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Which prayers the prayer service recorded as prayed, fetched in the request (with the caller's token) so
 * the queued write carries them. Missing data never takes a reward back: without it nothing is synced.
 */
@Service
@RequiredArgsConstructor
public class PrayerOutcomeSource {
    private static final Logger logger = LoggerFactory.getLogger(PrayerOutcomeSource.class);

    private final PrayerServiceClient prayer;

    /** @throws ApiException 503 {@code prayer_unavailable} when the prayer service does not answer */
    public List<PrayerOutcome> rewarded(Jwt caller) {
        try {
            return prayer.rewardedOutcomes(caller.getTokenValue());
        } catch (UpstreamUnavailableException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "prayer_unavailable",
                    "Prayer records are unavailable right now; try again.", exception.getMessage(), exception);
        }
    }

    /** The rewarded prayers, or null (with a warning) when the prayer service does not answer. */
    public List<PrayerOutcome> rewardedOrNull(Jwt caller) {
        try {
            return prayer.rewardedOutcomes(caller.getTokenValue());
        } catch (UpstreamUnavailableException exception) {
            logger.warn("Prayer rewards not synced: {}", exception.getMessage());
            return null;
        }
    }
}
