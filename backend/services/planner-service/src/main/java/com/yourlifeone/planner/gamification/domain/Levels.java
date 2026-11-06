package com.yourlifeone.planner.gamification.domain;

/** Levels grow with the square root of XP: level n starts at 25 n² XP. */
public final class Levels {
    private static final int XP_PER_LEVEL_SQUARED = 25;
    private static final int LEGEND = 20;
    private static final int RELENTLESS = 15;
    private static final int CONSISTENT = 10;
    private static final int FOCUSED = 5;

    private Levels() {
    }

    public static int levelFromXp(long totalXp) {
        return Math.max(1, (int) Math.floor(Math.sqrt(totalXp / (double) XP_PER_LEVEL_SQUARED)));
    }

    public static String titleForLevel(int level) {
        if (level >= LEGEND) return "Legend";
        if (level >= RELENTLESS) return "Relentless";
        if (level >= CONSISTENT) return "Consistent";
        if (level >= FOCUSED) return "Focused";
        return "Beginner";
    }
}
