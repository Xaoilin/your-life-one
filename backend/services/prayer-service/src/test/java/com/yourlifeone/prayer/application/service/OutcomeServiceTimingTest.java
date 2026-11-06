package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.OutcomeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutcomeServiceTimingTest {
    private final UUID user = UUID.randomUUID();
    private final OutcomeRepository repository = mock(OutcomeRepository.class);
    private final PrayerPersistenceMapper mapper = Mappers.getMapper(PrayerPersistenceMapper.class);
    private final PrayerProfileService profiles = mock(PrayerProfileService.class);
    private final OutcomeTimingPolicy policy = mock(OutcomeTimingPolicy.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);
    private final OutcomeService service = new OutcomeService(repository, mapper, profiles, policy, clock);

    @Test
    void newUntimedWritesHaveConservativeDefaultsAndExplicitPromptStaysExplicit() {
        when(repository.insertIfAbsent(any())).thenReturn(1);
        assertThat(record(OutcomeStatus.ON_TIME, null).timing()).isEqualTo(OutcomeTiming.WITHIN_WINDOW);
        assertThat(record(OutcomeStatus.LATE, null).timing()).isEqualTo(OutcomeTiming.OUTSIDE_WINDOW);
        assertThat(record(OutcomeStatus.MISSED, null).timing()).isNull();
        assertThat(record(OutcomeStatus.ON_TIME, OutcomeTiming.PROMPT).timing()).isEqualTo(OutcomeTiming.PROMPT);
    }

    @Test
    void rejectsEveryStatusTimingMismatchBeforeWriting() {
        for (var status : new OutcomeStatus[]{OutcomeStatus.ON_TIME, OutcomeStatus.LATE, OutcomeStatus.MISSED}) {
            for (var timing : OutcomeTiming.values()) {
                if (timing.matches(status)) continue;
                assertThatThrownBy(() -> record(status, timing)).isInstanceOf(ApiException.class);
            }
        }
        verifyNoInteractions(repository, profiles, policy);
    }

    @Test
    void timingOnlyCorrectionsPreserveTheOneTimeRewardAndUntimedCorrectionsPreserveTiming() {
        var existing = Outcome.builder().id(UUID.randomUUID()).date(LocalDate.parse("2026-11-05"))
                .prayer(PrayerName.DHUHR).status(OutcomeStatus.ON_TIME).timing(OutcomeTiming.WITHIN_WINDOW)
                .recordedAt(clock.instant()).rewarded(true).build();
        when(repository.findByIdAndUserId(existing.id(), user)).thenReturn(Optional.of(mapper.toEntity(existing, user)));
        var changed = service.correct(user, existing.id(), OutcomeStatus.ON_TIME, "history", OutcomeTiming.PROMPT);
        assertThat(changed.firstReward()).isFalse();
        assertThat(changed.outcome().rewarded()).isTrue();
        assertThat(changed.outcome().timing()).isEqualTo(OutcomeTiming.PROMPT);
        var unchanged = service.correct(user, existing.id(), OutcomeStatus.ON_TIME, "history");
        assertThat(unchanged.outcome().timing()).isEqualTo(OutcomeTiming.WITHIN_WINDOW);
        var legacy = existing.toBuilder().timing(null).build();
        when(repository.findByIdAndUserId(existing.id(), user)).thenReturn(Optional.of(mapper.toEntity(legacy, user)));
        assertThat(service.correct(user, existing.id(), OutcomeStatus.ON_TIME, "history").outcome().timing()).isNull();
        assertThat(service.correct(user, existing.id(), OutcomeStatus.LATE, "history").outcome().timing())
                .isEqualTo(OutcomeTiming.OUTSIDE_WINDOW);
    }

    private Outcome record(OutcomeStatus status, OutcomeTiming timing) {
        return service.record(user, LocalDate.parse("2026-11-05"), PrayerName.DHUHR, status, "test", null, timing).outcome();
    }
}
