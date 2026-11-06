package com.yourlifeone.planner.tasks.api.response;

import java.time.LocalDate;

public record RecurrenceResponse(String frequency, LocalDate lastReset) {
}
