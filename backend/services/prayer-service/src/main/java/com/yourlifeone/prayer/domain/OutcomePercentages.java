package com.yourlifeone.prayer.domain;

/** Share of classified outcomes; the three values always add up to 100, or are all 0. */
public record OutcomePercentages(int onTime, int late, int missed) {
    public static final OutcomePercentages NONE = new OutcomePercentages(0, 0, 0);
}
