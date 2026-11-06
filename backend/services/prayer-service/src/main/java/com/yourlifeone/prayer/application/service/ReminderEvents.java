package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.prayer.application.model.ReminderEventData;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes sent reminders as change events, which the live-update gateway streams to the user's open tabs.
 * Deadline reminders stay {@code prayer.reminder} (what the deployed app already shows); the bounded notices
 * are {@code prayer.notice}.
 */
@Component
@RequiredArgsConstructor
public class ReminderEvents {
    public static final String REMINDER = "prayer.reminder";
    public static final String NOTICE = "prayer.notice";

    private final ChangePublisher changes;
    private final JsonMapper json;

    public void publish(UUID userId, SentReminder reminder) {
        var type = reminder.kind() == ReminderKind.DEADLINE ? REMINDER : NOTICE;
        changes.publish(userId, type, json.valueToTree(data(reminder)));
    }

    private static ReminderEventData data(SentReminder reminder) {
        var deadline = reminder.kind() == ReminderKind.DEADLINE;
        return ReminderEventData.builder()
                .key(reminder.key())
                .kind(reminder.kind().label())
                .date(reminder.date())
                .prayer(reminder.prayer().label())
                .pillars(reminder.pillars().stream().map(Pillar::label).toList())
                .firesAt(reminder.firesAt())
                .expiresAt(reminder.expiresAt())
                .deadlineAt(deadline ? reminder.expiresAt() : null)
                .deadline(deadline ? reminder.deadline().label() : null)
                .timeZone(reminder.zone().getId())
                .reminderMinutes(reminder.reminderMinutes())
                .snoozedUntil(reminder.snoozedUntil())
                .snoozeCount(reminder.snoozeCount())
                .build();
    }
}
