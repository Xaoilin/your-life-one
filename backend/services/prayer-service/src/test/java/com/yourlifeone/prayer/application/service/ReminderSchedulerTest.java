package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Clock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReminderSchedulerTest {
    @Test
    void unavailableIdentityChecksSkipTheTickWithoutChangingProfiles() {
        var profiles = mock(PrayerProfileRepository.class);
        var evaluation = mock(ReminderEvaluationService.class);
        var events = mock(ReminderEvents.class);
        var migrator = mock(SchemaMigrator.class);
        when(profiles.findEligibleUserIds()).thenThrow(new DataAccessResourceFailureException("unavailable"));
        var meters = new SimpleMeterRegistry();
        var scheduler = new ReminderScheduler(profiles, evaluation, events, migrator,
                Clock.fixed(Instant.parse("2026-09-25T11:53:00Z"), ZoneOffset.UTC), meters);
        scheduler.bindHealth();

        assertDoesNotThrow(scheduler::sendDueReminders);

        verifyNoInteractions(evaluation, events);
        assertEquals(1, meters.get("yourlife.job.runs").tag("outcome", "failed").counter().count());
        assertEquals(0, meters.get("yourlife.job.last.success.timestamp").gauge().value());
    }
}
