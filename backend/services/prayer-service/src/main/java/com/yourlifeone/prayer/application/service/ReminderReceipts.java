package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReceiptGroup;
import com.yourlifeone.prayer.application.model.ReminderInputs;
import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.RecordedPrayers;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.infrastructure.persistence.entity.NoticeEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.ReminderEntity;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.ReminderPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.NoticeRepository;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ReminderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The reminders a user was sent that have not expired and are still wanted: the user still has those
 * reminders on, the prayer is not recorded, and a momentum pillar is not complete that day (a notice keeps
 * only its pillars still to do).
 */
@Component
@RequiredArgsConstructor
public class ReminderReceipts {
    private final ReminderRepository reminders;
    private final NoticeRepository notices;
    private final ReminderPersistenceMapper mapper;
    private final ReminderDays days;

    public List<ReceiptGroup> unexpired(UUID userId, PrayerProfile profile, List<MomentumPreference> preferences,
            Instant now) {
        return unexpired(userId, profile, preferences, now, Optional.empty());
    }

    public List<ReceiptGroup> unexpired(UUID userId, PrayerProfile profile, List<MomentumPreference> preferences,
            Instant now, ReminderInputs inputs) {
        return unexpired(userId, profile, preferences, now, Optional.of(inputs));
    }

    private List<ReceiptGroup> unexpired(UUID userId, PrayerProfile profile, List<MomentumPreference> preferences,
            Instant now, Optional<ReminderInputs> inputs) {
        if (!profile.enabled()) return List.of();
        var deadlineReceipts = profile.reminderEnabled() ? reminders.findByUserIdAndDeadlineAtAfter(userId, now) : List.<ReminderEntity>of();
        var noticeReceipts = notices.findByUserIdAndExpiresAtAfter(userId, now);
        if (deadlineReceipts.isEmpty() && noticeReceipts.isEmpty()) return List.of();

        var zone = inputs.map(ReminderInputs::zone).orElseGet(() -> days.zone(userId));
        var recorded = recorded(userId, deadlineReceipts, noticeReceipts, inputs);
        var groups = new ArrayList<ReceiptGroup>();
        deadlineReceipts.stream()
                .filter(receipt -> !recorded.contains(receipt.getPrayerDate(), PrayerName.fromLabel(receipt.getPrayerName())))
                .forEach(receipt -> groups.add(new ReceiptGroup(mapper.toSentReminder(receipt, zone,
                        profile.reminderMinutes()), List.of(), receipt.getResentAt() != null)));
        for (var receipts : byMoment(noticeReceipts)) {
            var wanted = stillWanted(receipts, profile, preferences, recorded);
            if (!wanted.isEmpty()) groups.add(noticeGroup(wanted, zone, profile.reminderMinutes()));
        }
        return groups;
    }

    /** Notice receipts grouped by the moment they fired, each group in pillar order. */
    private static Collection<List<NoticeEntity>> byMoment(List<NoticeEntity> receipts) {
        var groups = new LinkedHashMap<String, List<NoticeEntity>>();
        receipts.stream()
                .sorted(Comparator.comparing(NoticeEntity::getFiresAt)
                        .thenComparing(receipt -> Objects.requireNonNullElse(receipt.getPillar(), "")))
                .forEach(receipt -> groups.computeIfAbsent(String.join("|", receipt.getKind(),
                        receipt.getPrayerDate().toString(), receipt.getPrayerName(), receipt.getFiresAt().toString()),
                        moment -> new ArrayList<>()).add(receipt));
        return groups.values();
    }

    private static List<NoticeEntity> stillWanted(List<NoticeEntity> receipts, PrayerProfile profile,
            List<MomentumPreference> preferences, RecordedPrayers recorded) {
        var first = receipts.getFirst();
        if (ReminderKind.fromLabel(first.getKind()) == ReminderKind.OPPORTUNITY) {
            var prayer = PrayerName.fromLabel(first.getPrayerName());
            return profile.reminderEnabled() && !recorded.contains(first.getPrayerDate(), prayer) ? receipts : List.of();
        }
        Map<Pillar, MomentumPreference> byPillar = preferences.stream()
                .collect(Collectors.toMap(MomentumPreference::pillar, Function.identity()));
        return receipts.stream().filter(receipt -> {
            var preference = byPillar.get(Pillar.fromLabel(receipt.getPillar()));
            return preference != null && preference.enabled() && !preference.completeOn(receipt.getPrayerDate());
        }).toList();
    }

    private ReceiptGroup noticeGroup(List<NoticeEntity> receipts, ZoneId zone, int reminderMinutes) {
        return new ReceiptGroup(mapper.toSentReminder(receipts, zone, reminderMinutes),
                receipts.stream().map(NoticeEntity::getKey).toList(),
                receipts.stream().allMatch(receipt -> receipt.getResentAt() != null));
    }

    private RecordedPrayers recorded(UUID userId, List<ReminderEntity> deadlineReceipts, List<NoticeEntity> noticeReceipts,
            Optional<ReminderInputs> inputs) {
        var dates = Stream.concat(deadlineReceipts.stream().map(ReminderEntity::getPrayerDate),
                noticeReceipts.stream().map(NoticeEntity::getPrayerDate)).toList();
        LocalDate from = dates.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate to = dates.stream().max(Comparator.naturalOrder()).orElseThrow();
        return inputs.filter(pass -> pass.covers(from, to)).map(ReminderInputs::recorded)
                .orElseGet(() -> days.recorded(userId, from, to));
    }
}
