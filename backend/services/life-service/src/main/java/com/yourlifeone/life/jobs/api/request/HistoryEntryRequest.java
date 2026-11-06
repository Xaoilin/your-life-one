package com.yourlifeone.life.jobs.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Sourced evidence to append.
 *
 * @param id optional; chosen by the service when absent
 * @param date only when the source gives one
 */
public record HistoryEntryRequest(
        @Size(max = 256) String id,
        @NotNull @Size(max = 32) String kind,
        LocalDate date,
        @NotBlank @Size(max = 2000) String summary,
        @Size(max = 10000) String details,
        @Size(max = 2048) String evidenceUrl) {
}
