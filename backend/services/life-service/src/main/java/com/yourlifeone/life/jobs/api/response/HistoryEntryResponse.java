package com.yourlifeone.life.jobs.api.response;

import java.time.LocalDate;

public record HistoryEntryResponse(
        String id, String kind, LocalDate date, String summary, String details, String evidenceUrl) {
}
