package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReminderInputs;
import com.yourlifeone.prayer.domain.DueReminders;
import com.yourlifeone.prayer.domain.Reminder;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Decides which of a user's deadline reminders are due and claims each one once. */
@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository reminders;

    /** The reminders due now that were not sent before, each now recorded as sent. */
    public List<SentReminder> claimDue(UUID userId, int reminderMinutes, Instant now, ReminderInputs inputs) {
        return DueReminders.due(inputs.timetables(), inputs.recorded().keys(), reminderMinutes, now).stream()
                .filter(reminder -> reminders.claim(userId, reminder.date(), reminder.prayer().label(),
                        reminder.deadlineAt(), now))
                .map(reminder -> sent(reminder, now))
                .toList();
    }

    private static SentReminder sent(Reminder reminder, Instant now) {
        return SentReminder.builder()
                .kind(ReminderKind.DEADLINE)
                .date(reminder.date())
                .prayer(reminder.prayer())
                .firesAt(now)
                .expiresAt(reminder.deadlineAt())
                .deadline(reminder.deadline())
                .zone(reminder.zone())
                .reminderMinutes(reminder.reminderMinutes())
                .build();
    }
}
