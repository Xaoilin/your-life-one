package com.yourlifeone.planner.gamification.domain;

import java.time.LocalDate;
import java.util.Set;

/** A streak counts consecutive days with a rewarded completion, in the app's time zone. */
public final class Streaks {
    private static final Set<Integer> MILESTONES = Set.of(7, 14, 30, 60, 100);

    private Streaks() {
    }

    /** The profile's streak after a completion on {@code day}; a second completion that day changes nothing. */
    public static Profile afterCompletion(Profile profile, LocalDate day) {
        if (day.equals(profile.lastCompletionDate())) return profile;
        var continues = day.minusDays(1).equals(profile.lastCompletionDate());
        var streak = continues ? profile.currentStreak() + 1 : 1;
        return profile.toBuilder()
                .currentStreak(streak)
                .longestStreak(Math.max(profile.longestStreak(), streak))
                .lastCompletionDate(day)
                .build();
    }

    /** True when the last completion was before yesterday, so the streak has ended. */
    public static boolean isBroken(Profile profile, LocalDate today) {
        var last = profile.lastCompletionDate();
        return last != null && !last.equals(today) && !last.equals(today.minusDays(1));
    }

    public static boolean isMilestone(int streak) {
        return MILESTONES.contains(streak);
    }
}
