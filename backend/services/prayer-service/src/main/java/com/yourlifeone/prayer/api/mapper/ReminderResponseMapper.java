package com.yourlifeone.prayer.api.mapper;

import com.yourlifeone.prayer.api.request.MomentumReminderRequest;
import com.yourlifeone.prayer.api.response.MomentumReminderResponse;
import com.yourlifeone.prayer.api.response.MomentumRemindersResponse;
import com.yourlifeone.prayer.api.response.ReminderResponse;
import com.yourlifeone.prayer.api.response.RemindersResponse;
import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps reminders and momentum reminder preferences to and from the API. */
@Mapper
public interface ReminderResponseMapper {
    MomentumReminderResponse toResponse(MomentumPreference preference);

    MomentumPreference toDomain(MomentumReminderRequest request);

    List<MomentumPreference> toDomain(List<MomentumReminderRequest> requests);

    default MomentumRemindersResponse toMomentumResponse(List<MomentumPreference> preferences) {
        return new MomentumRemindersResponse(preferences.stream().map(this::toResponse).toList());
    }

    default RemindersResponse toResponse(List<SentReminder> active, List<MomentumPreference> momentum) {
        return new RemindersResponse(active.stream().map(this::toResponse).toList(),
                momentum.stream().map(this::toResponse).toList());
    }

    default ReminderResponse toResponse(SentReminder reminder) {
        var deadline = reminder.kind() == ReminderKind.DEADLINE;
        return ReminderResponse.builder()
                .key(reminder.key())
                .kind(reminder.kind())
                .date(reminder.date())
                .prayer(reminder.prayer())
                .pillars(reminder.pillars())
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
