package com.yourlifeone.planner.tasks.domain;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The daily reset reopens each completed habit once per new day, in the right time zone's date. */
class HabitResetTest {
    private static final LocalDate MONDAY = LocalDate.parse("2026-09-28");
    private static final LocalDate SATURDAY = LocalDate.parse("2026-09-26");

    private static Task habit(String category, String recurrence, LocalDate lastReset) {
        return Task.builder().id("h").title("Habit").category(category).priority("high").completed(true)
                .completedAt(Instant.parse("2026-09-27T08:00:00Z")).recurrence(recurrence).lastReset(lastReset)
                .prayerName("prayer".equals(category) ? "Fajr" : null).build();
    }

    @Test
    void aHabitCompletedYesterdayReopensToday() {
        assertEquals(MONDAY, HabitReset.reopensOn(habit("daily", "daily", MONDAY.minusDays(1)), MONDAY, MONDAY));
    }

    @Test
    void aHabitResetOrCompletedTodayStaysAsItIs() {
        assertNull(HabitReset.reopensOn(habit("daily", "daily", MONDAY), MONDAY, MONDAY));
    }

    @Test
    void prayerHabitsFollowThePrayerDateAndWaitWhileItIsUnknown() {
        var fajr = habit("prayer", "daily", MONDAY.minusDays(1));

        assertNull(HabitReset.reopensOn(fajr, MONDAY, null));
        assertNull(HabitReset.reopensOn(fajr, MONDAY, MONDAY.minusDays(1)));
        assertEquals(MONDAY, HabitReset.reopensOn(fajr, MONDAY.minusDays(1), MONDAY));
    }

    @Test
    void weekdayHabitsDoNotReopenAtTheWeekend() {
        assertNull(HabitReset.reopensOn(habit("daily", "weekdays", SATURDAY.minusDays(1)), SATURDAY, SATURDAY));
    }

    @Test
    void openTasksAndOneOffTasksAreLeftAlone() {
        var open = habit("daily", "daily", MONDAY.minusDays(1)).toBuilder().completed(false).completedAt(null).build();
        var oneOff = habit("task", null, null);

        assertNull(HabitReset.reopensOn(open, MONDAY, MONDAY));
        assertNull(HabitReset.reopensOn(oneOff, MONDAY, MONDAY));
    }
}
