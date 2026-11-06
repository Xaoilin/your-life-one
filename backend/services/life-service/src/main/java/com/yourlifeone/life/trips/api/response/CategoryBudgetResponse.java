package com.yourlifeone.life.trips.api.response;

public record CategoryBudgetResponse(String category, long forecast, long paid, int count, int uncostedCount) {
}
