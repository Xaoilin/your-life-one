package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.OutcomeRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReminderDaysTest {
    private final UserScheduleService schedules = mock(UserScheduleService.class);
    private final OutcomeRepository outcomes = mock(OutcomeRepository.class);
    private final PrayerPersistenceMapper mapper = mock(PrayerPersistenceMapper.class);
    private final ReminderDays days = new ReminderDays(schedules, outcomes, mapper);
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-25T01:00:00Z");

    @Test
    void anUnknownScheduleKeepsTheUtcReceiptFallbackWithoutReadingOutcomes() {
        when(schedules.latest(user)).thenReturn(Optional.empty());
        var inputs = days.forPass(user, now, true);
        assertEquals(List.of(), inputs.timetables());
        assertEquals(ZoneOffset.UTC, inputs.zone());
        assertFalse(inputs.covers(Fixtures.DAY, Fixtures.DAY));
        verifyNoInteractions(outcomes, mapper);
    }

    @Test
    void missingYesterdayUsesTodaysKnownScheduleWhileReceiptsKeepTheLatestZone() {
        var today = Fixtures.london(Fixtures.DAY);
        var latest = Fixtures.london(Fixtures.DAY.plusDays(1)).toBuilder().zone(ZoneId.of("Asia/Dubai")).build();
        when(schedules.latest(user)).thenReturn(Optional.of(latest));
        when(schedules.knownDays(user, Fixtures.DAY.minusDays(1), Fixtures.DAY)).thenReturn(Map.of(Fixtures.DAY, today));
        when(outcomes.findByUserIdAndPrayerDateBetween(user, Fixtures.DAY.minusDays(1), Fixtures.DAY)).thenReturn(List.of());
        var inputs = days.forPass(user, now, true);
        assertEquals(List.of(today.onDate(Fixtures.DAY.minusDays(1)), today), inputs.timetables());
        assertEquals(latest.zone(), inputs.zone());
        verify(schedules).latest(user);
        verify(schedules).knownDays(user, Fixtures.DAY.minusDays(1), Fixtures.DAY);
        verify(outcomes).findByUserIdAndPrayerDateBetween(user, Fixtures.DAY.minusDays(1), Fixtures.DAY);
    }

    @Test
    void disabledDeadlinesNeedOnlyTodaysSharedRange() {
        var latest = Fixtures.london(Fixtures.DAY);
        when(schedules.latest(user)).thenReturn(Optional.of(latest));
        when(schedules.knownDays(user, Fixtures.DAY, Fixtures.DAY)).thenReturn(Map.of());
        when(outcomes.findByUserIdAndPrayerDateBetween(user, Fixtures.DAY, Fixtures.DAY)).thenReturn(List.of());
        var inputs = days.forPass(user, now, false);
        assertEquals(List.of(latest), inputs.timetables());
        verify(outcomes).findByUserIdAndPrayerDateBetween(user, Fixtures.DAY, Fixtures.DAY);
    }
}
