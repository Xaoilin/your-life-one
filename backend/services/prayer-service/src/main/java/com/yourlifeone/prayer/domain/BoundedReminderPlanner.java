package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which bounded notices a day calls for, ported from the React app's {@code buildBoundedReminderPlan}:
 * <ul>
 *   <li>each prayer not yet recorded has an opportunity notice from its start until its deadline or the end of
 *       the opportunity window, whichever is first;</li>
 *   <li>each enabled pillar not yet complete that day prompts at the start of each of its anchor prayers,
 *       except inside the non-prayer quiet hours, until its next anchor or the start of the quiet hours;</li>
 *   <li>pillars prompting at the same instant share one notice, which ends when the first of them ends.</li>
 * </ul>
 */
public final class BoundedReminderPlanner {
    private static final Comparator<PlannedNotice> ORDER = Comparator.comparing(PlannedNotice::firesAt)
            .thenComparing(notice -> notice.kind().label())
            .thenComparing(PlannedNotice::key);

    private BoundedReminderPlanner() {
    }

    /**
     * @param recorded prayers of the day already recorded, whatever their status
     * @param opportunities whether prayer opportunity notices are wanted (deadline reminders are on)
     */
    public static List<PlannedNotice> plan(ScheduleDay day, Set<PrayerName> recorded,
            Collection<MomentumPreference> preferences, boolean opportunities, NoticePolicy policy) {
        var plans = new ArrayList<PlannedNotice>();
        if (opportunities) plans.addAll(opportunities(day, recorded, policy));
        plans.addAll(momentum(day, preferences, policy));
        plans.sort(ORDER);
        return plans;
    }

    /** The notices active at {@code now}. */
    public static List<PlannedNotice> activeAt(List<PlannedNotice> plans, Instant now) {
        return plans.stream().filter(plan -> plan.isActiveAt(now)).toList();
    }

    private static List<PlannedNotice> opportunities(ScheduleDay day, Set<PrayerName> recorded, NoticePolicy policy) {
        var notices = new ArrayList<PlannedNotice>();
        for (var prayer : PrayerName.values()) {
            if (recorded.contains(prayer)) continue;
            day.window(prayer).ifPresent(window -> notices.add(PlannedNotice.builder()
                    .kind(ReminderKind.OPPORTUNITY)
                    .date(day.date())
                    .prayer(prayer)
                    .firesAt(window.startsAt())
                    .expiresAt(earlier(window.deadlineAt(), window.startsAt().plus(policy.opportunityWindow())))
                    .build()));
        }
        return notices;
    }

    private static List<PlannedNotice> momentum(ScheduleDay day, Collection<MomentumPreference> preferences,
            NoticePolicy policy) {
        var quietStart = ZonedDateTime.of(day.date(), LocalTime.of(policy.quietHoursStartHour(), 0), day.zone())
                .toInstant();
        var groups = new LinkedHashMap<Instant, MomentumGroup>();
        preferences.stream()
                .filter(preference -> preference.enabled() && !preference.completeOn(day.date()))
                .sorted(Comparator.comparing(MomentumPreference::pillar))
                .forEach(preference -> addPrompts(day, preference, quietStart, policy, groups));
        return groups.values().stream().map(group -> group.toNotice(day.date())).toList();
    }

    private static void addPrompts(ScheduleDay day, MomentumPreference preference, Instant quietStart,
            NoticePolicy policy, Map<Instant, MomentumGroup> groups) {
        var anchors = anchors(day, preference.afterPrayers());
        for (int index = 0; index < anchors.size(); index++) {
            var anchor = anchors.get(index);
            if (isQuiet(anchor.startsAt(), day, policy)) continue;
            var expiresAt = index + 1 < anchors.size() ? earlier(anchors.get(index + 1).startsAt(), quietStart) : quietStart;
            if (!expiresAt.isAfter(anchor.startsAt())) continue;
            groups.computeIfAbsent(anchor.startsAt(), firesAt -> new MomentumGroup(anchor.prayer(), firesAt, expiresAt))
                    .add(preference.pillar(), expiresAt);
        }
    }

    /** The anchor prayers' starts, earliest first; a prayer the timetable cannot place is left out. */
    private static List<PrayerWindow> anchors(ScheduleDay day, List<PrayerName> prayers) {
        return prayers.stream()
                .map(day::window)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(PrayerWindow::startsAt))
                .toList();
    }

    private static boolean isQuiet(Instant instant, ScheduleDay day, NoticePolicy policy) {
        return policy.isQuietHour(instant.atZone(day.zone()).getHour());
    }

    private static Instant earlier(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    /** Pillars prompting at one instant. */
    private static final class MomentumGroup {
        private final PrayerName prayer;
        private final Instant firesAt;
        private final List<Pillar> pillars = new ArrayList<>();
        private Instant expiresAt;

        MomentumGroup(PrayerName prayer, Instant firesAt, Instant expiresAt) {
            this.prayer = prayer;
            this.firesAt = firesAt;
            this.expiresAt = expiresAt;
        }

        void add(Pillar pillar, Instant pillarExpiresAt) {
            pillars.add(pillar);
            expiresAt = earlier(expiresAt, pillarExpiresAt);
        }

        PlannedNotice toNotice(LocalDate date) {
            return PlannedNotice.builder()
                    .kind(ReminderKind.MOMENTUM)
                    .date(date)
                    .prayer(prayer)
                    .pillars(pillars)
                    .firesAt(firesAt)
                    .expiresAt(expiresAt)
                    .build();
        }
    }
}
