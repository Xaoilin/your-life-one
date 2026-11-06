package com.yourlifeone.planner.gamification.domain;

/** A profile after a rewarded completion, and what the completion earned. */
public record Rewarded(Profile profile, Reward reward) {
}
