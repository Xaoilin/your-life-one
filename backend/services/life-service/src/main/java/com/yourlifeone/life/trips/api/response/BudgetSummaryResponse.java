package com.yourlifeone.life.trips.api.response;

import java.util.List;

public record BudgetSummaryResponse(long forecastTotal, long paidTotal, long remaining, int uncostedBookingCount,
        List<CategoryBudgetResponse> byCategory) {
}
