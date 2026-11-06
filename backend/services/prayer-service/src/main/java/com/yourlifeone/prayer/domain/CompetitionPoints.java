package com.yourlifeone.prayer.domain;

/** Owner-selected competition policy, separate from the planner's one-time XP reward. */
public final class CompetitionPoints {
    public static final int AVAILABLE_PER_PRAYER = 12;

    private CompetitionPoints() {
    }

    public static int earned(Outcome outcome) {
        if (!outcome.status().isCompleted()) return 0;
        // A null timing is historical: retain its original full completion credit.
        if (outcome.timing() == null) return AVAILABLE_PER_PRAYER;
        return switch (outcome.timing()) {
            case PROMPT -> 12;
            case WITHIN_WINDOW -> 8;
            case OUTSIDE_WINDOW -> 2;
        };
    }
}
