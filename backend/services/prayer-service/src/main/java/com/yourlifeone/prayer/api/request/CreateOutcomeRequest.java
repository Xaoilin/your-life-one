package com.yourlifeone.prayer.api.request;

import lombok.Builder;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import com.yourlifeone.prayer.domain.PrayerName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** @param source where the user recorded it, e.g. dashboard, tasks, reminder, chat */
@Builder
public record CreateOutcomeRequest(
        @NotNull LocalDate date,
        @NotNull PrayerName prayer,
        @NotNull OutcomeStatus status,
        @Size(max = 32) String source,
        @Size(max = 128) String taskId,
        OutcomeTiming timing) {
}
