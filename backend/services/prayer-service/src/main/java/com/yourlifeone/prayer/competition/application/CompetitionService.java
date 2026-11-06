package com.yourlifeone.prayer.competition.application;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.service.StatsService;
import com.yourlifeone.prayer.domain.CompetitionPoints;
import com.yourlifeone.prayer.competition.api.response.CompetitionResponse;
import com.yourlifeone.prayer.competition.api.response.CompetitionSummary;
import com.yourlifeone.prayer.competition.config.CompetitionProperties;
import com.yourlifeone.prayer.competition.infrastructure.CompetitionPeer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompetitionService {
    private final StatsService stats;
    private final Clock clock;
    private final CompetitionProperties properties;
    private final CompetitionPeer peer;

    public CompetitionResponse comparison(UUID user, LocalDate from, LocalDate to) {
        var self = summary(user, from, to);
        if (!properties.canReadPeer() || !user.equals(properties.ownerId())) {
            return new CompetitionResponse("not_connected", self, null);
        }
        var partner = peer.read(from, to);
        return new CompetitionResponse(partner == null ? "unavailable" : "ready", self, partner);
    }

    public CompetitionSummary summary(UUID user, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 30) {
            throw ApiException.badRequest("invalid_competition_range", "Choose a day, week or month of at most 31 days.");
        }
        var result = stats.stats(user, from, to);
        var tally = result.overall();
        return CompetitionSummary.builder().from(from).to(to).asOf(clock.instant())
                .onTime(tally.onTime()).late(tally.late()).missed(tally.missed())
                .pointsEarned(result.competitionPoints())
                .pointsAvailable(CompetitionPoints.AVAILABLE_PER_PRAYER * tally.classifiedTotal())
                .allPrayed(result.allPrayed()).build();
    }
}
