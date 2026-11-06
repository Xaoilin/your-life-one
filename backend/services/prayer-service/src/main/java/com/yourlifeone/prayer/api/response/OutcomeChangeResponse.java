package com.yourlifeone.prayer.api.response;

/** @param firstReward true only the first time this prayer on this date is completed: award XP then */
public record OutcomeChangeResponse(OutcomeResponse outcome, boolean firstReward) {
}
