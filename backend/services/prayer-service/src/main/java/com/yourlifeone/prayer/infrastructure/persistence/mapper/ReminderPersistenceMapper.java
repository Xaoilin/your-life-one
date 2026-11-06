package com.yourlifeone.prayer.infrastructure.persistence.mapper;

import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PlannedNotice;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.entity.MomentumReminderEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.NoticeEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.ReminderEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import org.mapstruct.Mapper;

/** Converts reminder receipts and momentum preferences between rows and domain objects. */
@Mapper
public interface ReminderPersistenceMapper {
    /** A deadline reminder as sent: it fired when it was sent and expires at the deadline. */
    default SentReminder toSentReminder(ReminderEntity receipt, ZoneId zone, int reminderMinutes) {
        var prayer = PrayerName.fromLabel(receipt.getPrayerName());
        return SentReminder.builder()
                .kind(ReminderKind.DEADLINE)
                .date(receipt.getPrayerDate())
                .prayer(prayer)
                .firesAt(receipt.getSentAt())
                .expiresAt(receipt.getDeadlineAt())
                .deadline(prayer.deadline())
                .zone(zone)
                .reminderMinutes(reminderMinutes)
                .snoozedUntil(receipt.getSnoozedUntil())
                .snoozeCount(receipt.getSnoozeCount())
                .build();
    }

    /**
     * One notice from its receipts (one per pillar of a momentum notice): it ends when the first of them
     * ends and counts as snoozed when any of them is.
     */
    default SentReminder toSentReminder(List<NoticeEntity> receipts, ZoneId zone, int reminderMinutes) {
        var first = receipts.getFirst();
        return SentReminder.builder()
                .kind(ReminderKind.fromLabel(first.getKind()))
                .date(first.getPrayerDate())
                .prayer(PrayerName.fromLabel(first.getPrayerName()))
                .pillars(receipts.stream().map(NoticeEntity::getPillar).filter(Objects::nonNull).map(Pillar::fromLabel)
                        .sorted().toList())
                .firesAt(first.getFiresAt())
                .expiresAt(receipts.stream().map(NoticeEntity::getExpiresAt).min(Comparator.naturalOrder()).orElseThrow())
                .zone(zone)
                .reminderMinutes(reminderMinutes)
                .snoozedUntil(receipts.stream().map(NoticeEntity::getSnoozedUntil).filter(Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(null))
                .snoozeCount(receipts.stream().mapToInt(NoticeEntity::getSnoozeCount).max().orElse(0))
                .build();
    }

    /** The notice's receipts, one per pillar of a momentum notice. */
    default List<NoticeEntity> toReceipts(UUID userId, PlannedNotice notice, Instant sentAt) {
        var keys = notice.receiptKeys();
        var pillars = notice.kind() == ReminderKind.MOMENTUM ? notice.pillars() : null;
        return IntStream.range(0, keys.size()).mapToObj(index -> NoticeEntity.builder()
                .userId(userId)
                .key(keys.get(index))
                .kind(notice.kind().label())
                .prayerDate(notice.date())
                .prayerName(notice.prayer().label())
                .pillar(pillars == null ? null : pillars.get(index).label())
                .firesAt(notice.firesAt())
                .expiresAt(notice.expiresAt())
                .sentAt(sentAt)
                .build()).toList();
    }

    default MomentumPreference toDomain(MomentumReminderEntity entity) {
        return MomentumPreference.builder()
                .pillar(Pillar.fromLabel(entity.getPillar()))
                .enabled(entity.isEnabled())
                .afterPrayers(entity.getAfterPrayers().stream().map(PrayerName::fromLabel).toList())
                .completedOn(entity.getCompletedOn())
                .build();
    }

    default MomentumReminderEntity toEntity(UUID userId, MomentumPreference preference, Instant updatedAt) {
        return MomentumReminderEntity.builder()
                .userId(userId)
                .pillar(preference.pillar().label())
                .enabled(preference.enabled())
                .afterPrayers(preference.afterPrayers().stream().map(PrayerName::label).toList())
                .completedOn(preference.completedOn())
                .updatedAt(updatedAt)
                .build();
    }
}
