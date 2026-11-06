package com.yourlifeone.prayer.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static com.yourlifeone.prayer.domain.Fixtures.london;
import static com.yourlifeone.prayer.domain.Fixtures.londonTimes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules of the React app's bounded reminders ({@code boundedReminders.ts}). London on 25 Sep 2026 (BST):
 * Fajr starts 04:12Z, Dhuhr 11:52Z, Asr 15:08Z, Maghrib 18:12Z, Isha 19:18Z; quiet hours start 22:00 BST (21:00Z).
 */
class BoundedReminderPlannerTest {
    private static final NoticePolicy POLICY = NoticePolicy.builder()
            .opportunityWindow(Duration.ofMinutes(30)).quietHoursStartHour(22).quietHoursEndHour(8).build();
    private static final MomentumPreference LEARN = preference(Pillar.LEARN, PrayerName.DHUHR, PrayerName.MAGHRIB,
            PrayerName.ISHA);
    private static final MomentumPreference MOVE = preference(Pillar.MOVE, PrayerName.ASR, PrayerName.MAGHRIB,
            PrayerName.ISHA);

    private static MomentumPreference preference(Pillar pillar, PrayerName... prayers) {
        return MomentumPreference.builder().pillar(pillar).enabled(true).afterPrayers(List.of(prayers)).build();
    }

    private static List<PlannedNotice> plan(Set<PrayerName> recorded, List<MomentumPreference> preferences) {
        return BoundedReminderPlanner.plan(london(DAY), recorded, preferences, true, POLICY);
    }

    private static List<PlannedNotice> ofKind(List<PlannedNotice> plans, ReminderKind kind) {
        return plans.stream().filter(plan -> plan.kind() == kind).toList();
    }

    @Test
    void everyUnrecordedPrayerHasOneOpportunityFromItsStartForTheWindow() {
        var opportunities = ofKind(plan(Set.of(), List.of()), ReminderKind.OPPORTUNITY);

        assertEquals(5, opportunities.size());
        var fajr = opportunities.getFirst();
        assertEquals("opportunity:2026-09-25:Fajr", fajr.key());
        assertEquals(Instant.parse("2026-09-25T04:12:00Z"), fajr.firesAt());
        assertEquals(Instant.parse("2026-09-25T04:42:00Z"), fajr.expiresAt());
    }

    @Test
    void anOpportunityEndsAtTheDeadlineWhenThatComesFirst() {
        var times = londonTimes();
        times.put(Timing.ISHA, LocalTime.of(19, 30));
        var day = london(DAY).toBuilder().times(times).build();

        var maghrib = BoundedReminderPlanner.plan(day, Set.of(), List.of(), true, POLICY).stream()
                .filter(plan -> plan.prayer() == PrayerName.MAGHRIB).findFirst().orElseThrow();

        assertEquals(Instant.parse("2026-09-25T18:30:00Z"), maghrib.expiresAt());
    }

    @Test
    void aRecordedPrayerHasNoOpportunityAndOpportunitiesFollowTheDeadlineReminderSwitch() {
        var opportunities = ofKind(plan(Set.of(PrayerName.DHUHR), List.of()), ReminderKind.OPPORTUNITY);
        assertEquals(4, opportunities.size());
        assertTrue(opportunities.stream().noneMatch(plan -> plan.prayer() == PrayerName.DHUHR));

        assertEquals(List.of(), BoundedReminderPlanner.plan(london(DAY), Set.of(), List.of(), false, POLICY));
    }

    @Test
    void pillarsPromptingAtTheSameInstantShareOneNoticeThatEndsWithTheFirstOfThem() {
        var momentum = ofKind(plan(Set.of(), List.of(LEARN, MOVE)), ReminderKind.MOMENTUM);

        assertEquals(List.of("11:52Z learn until 18:12Z", "15:08Z move until 18:12Z",
                "18:12Z learn+move until 19:18Z", "19:18Z learn+move until 21:00Z"), momentum.stream()
                .map(plan -> time(plan.firesAt()) + " " + String.join("+", plan.pillars().stream().map(Pillar::label)
                        .toList()) + " until " + time(plan.expiresAt()))
                .toList());
        var maghrib = momentum.get(2);
        assertEquals("momentum:2026-09-25:" + Instant.parse("2026-09-25T18:12:00Z").toEpochMilli() + ":learn+move",
                maghrib.key());
        assertEquals(PrayerName.MAGHRIB, maghrib.prayer());
        assertEquals(List.of(maghrib.key().replace("learn+move", "learn"), maghrib.key().replace("learn+move", "move")),
                maghrib.receiptKeys());
    }

    @Test
    void anchorsInsideTheQuietHoursNeverPrompt() {
        var times = londonTimes();
        times.put(Timing.ISHA, LocalTime.of(22, 30));
        times.put(Timing.MIDNIGHT, LocalTime.of(23, 50));
        var day = london(DAY).toBuilder().times(times).build();
        var learn = preference(Pillar.LEARN, PrayerName.FAJR, PrayerName.MAGHRIB, PrayerName.ISHA);

        var momentum = ofKind(BoundedReminderPlanner.plan(day, Set.of(), List.of(learn), true, POLICY),
                ReminderKind.MOMENTUM);

        assertEquals(1, momentum.size());
        assertEquals(PrayerName.MAGHRIB, momentum.getFirst().prayer());
        assertEquals(Instant.parse("2026-09-25T21:00:00Z"), momentum.getFirst().expiresAt());
    }

    @Test
    void aPillarCompleteForTheDayOrSwitchedOffDoesNotPrompt() {
        var learnDone = MomentumPreference.builder().pillar(Pillar.LEARN).enabled(true)
                .afterPrayers(LEARN.afterPrayers()).completedOn(DAY).build();
        var moveOff = MomentumPreference.builder().pillar(Pillar.MOVE).enabled(false)
                .afterPrayers(MOVE.afterPrayers()).build();

        assertEquals(List.of(), ofKind(plan(Set.of(), List.of(learnDone, moveOff)), ReminderKind.MOMENTUM));
        var yesterday = learnDone.toBuilder().completedOn(DAY.minusDays(1)).build();
        assertEquals(3, ofKind(plan(Set.of(), List.of(yesterday)), ReminderKind.MOMENTUM).size());
    }

    @Test
    void onlyNoticesBetweenFiringAndExpiryAreActive() {
        var plans = plan(Set.of(), List.of(LEARN, MOVE));
        var fajr = plans.getFirst();

        assertEquals(List.of(fajr), BoundedReminderPlanner.activeAt(plans, fajr.firesAt().plusSeconds(1)));
        assertEquals(List.of(), BoundedReminderPlanner.activeAt(plans, fajr.firesAt().minusSeconds(1)));
        assertEquals(List.of(), BoundedReminderPlanner.activeAt(plans, fajr.expiresAt()));
    }

    private static String time(Instant instant) {
        return instant.toString().substring("2026-09-25T".length(), "2026-09-25T00:00".length()) + "Z";
    }
}
