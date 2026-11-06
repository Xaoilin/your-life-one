package com.yourlifeone.prayer.api.response;

import java.util.Map;

/** @param perPrayer keyed by prayer name, e.g. "Fajr" */
public record StatsResponse(TallyResponse overall, int trackedDays, Map<String, TallyResponse> perPrayer) {
}
