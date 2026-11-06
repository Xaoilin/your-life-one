package com.yourlifeone.prayer.application.model;

import com.yourlifeone.prayer.domain.Outcome;

/** @param firstReward true exactly once per user, date and prayer: the React app awards XP then */
public record OutcomeChange(Outcome outcome, boolean firstReward) {
}
