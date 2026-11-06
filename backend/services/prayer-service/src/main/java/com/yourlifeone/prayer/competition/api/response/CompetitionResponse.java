package com.yourlifeone.prayer.competition.api.response;

/** Partner data is absent when disabled or unavailable; it is never replaced with zero. */
public record CompetitionResponse(String status, CompetitionSummary self, CompetitionSummary partner) {
}
