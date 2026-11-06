package com.yourlifeone.prayer.competition;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.service.StatsService;
import com.yourlifeone.prayer.competition.application.CompetitionService;
import com.yourlifeone.prayer.competition.config.CompetitionProperties;
import com.yourlifeone.prayer.competition.infrastructure.CompetitionPeer;
import com.yourlifeone.prayer.domain.OutcomeStats;
import com.yourlifeone.prayer.domain.OutcomeTally;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CompetitionServiceTest {
    private final UUID owner = UUID.randomUUID();
    private final StatsService stats = mock(StatsService.class);
    private final CompetitionPeer peer = mock(CompetitionPeer.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
    private final LocalDate from = LocalDate.parse("2026-11-01");
    private final LocalDate to = LocalDate.parse("2026-11-30");
    private final CompetitionProperties properties = new CompetitionProperties(owner, "a".repeat(40),
            URI.create("https://partner.example.test/api/prayer/v1/competition/summary"), "b".repeat(40));
    private final CompetitionService service = new CompetitionService(stats, clock, properties, peer);

    @BeforeEach
    void tally() {
        var tally = OutcomeTally.builder().onTime(8).late(1).missed(1)
                .pending(3).unclassified(2).classifiedTotal(10).opportunities(20).build();
        when(stats.stats(any(), any(), any())).thenReturn(OutcomeStats.builder().overall(tally).trackedDays(4).perPrayer(Map.of()).competitionPoints(100).build());
    }

    @Test
    void sharesOnlyClassifiedCountsAndNeverPendingInformation() {
        var summary = service.summary(owner, from, to);
        assertThat(summary.onTime()).isEqualTo(8);
        assertThat(summary.late()).isEqualTo(1);
        assertThat(summary.missed()).isEqualTo(1);
        assertThat(summary.asOf()).isEqualTo(clock.instant());
        assertThat(summary.pointsEarned()).isEqualTo(100);
        assertThat(summary.pointsAvailable()).isEqualTo(120);
    }

    @Test
    void restrictsThePartnerToTheConfiguredAccount() {
        assertThat(service.comparison(UUID.randomUUID(), from, to).status()).isEqualTo("not_connected");
        verifyNoInteractions(peer);
    }

    @Test
    void doesNotManufactureZeroWhenThePartnerIsUnavailable() {
        var result = service.comparison(owner, from, to);
        assertThat(result.status()).isEqualTo("unavailable");
        assertThat(result.partner()).isNull();
        var summary = service.summary(owner, from, to);
        when(peer.read(from, to)).thenReturn(summary);
        assertThat(service.comparison(owner, from, to).status()).isEqualTo("ready");
    }

    @Test
    void disabledSharingDoesNotReadThePeer() {
        var disabled = new CompetitionService(stats, clock, new CompetitionProperties(null, null, null, null), peer);
        assertThat(disabled.comparison(owner, from, to).status()).isEqualTo("not_connected");
        verifyNoInteractions(peer);
        assertThat(properties.toString()).doesNotContain("a".repeat(40), "b".repeat(40));
    }

    @Test
    void boundsRequestsBeforeAnyDatabaseWork() {
        assertThatThrownBy(() -> service.summary(owner, to, from)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.summary(owner, from, from.plusDays(31))).isInstanceOf(ApiException.class);
    }
}
