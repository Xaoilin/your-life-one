package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReminderEvaluationServiceTest {
    @Test
    void aDeletedIdentityNeverReachesReminderEvaluationEvenFromAStaleCandidate() {
        var user = UUID.randomUUID();
        var profiles = mock(PrayerProfileRepository.class);
        var mapper = mock(PrayerPersistenceMapper.class);
        var deadlines = mock(ReminderService.class);
        var notices = mock(NoticeService.class);
        var resends = mock(ReminderResendService.class);
        var momentum = mock(MomentumReminderService.class);
        var days = mock(ReminderDays.class);
        when(profiles.lockIdentity(user)).thenReturn(Optional.empty());

        new ReminderEvaluationService(profiles, mapper, deadlines, notices, resends, momentum, days)
                .claimDue(user, Instant.parse("2026-09-25T11:53:00Z"));

        verify(profiles, never()).findById(user);
        verifyNoInteractions(mapper, deadlines, notices, resends, momentum, days);
    }
}
