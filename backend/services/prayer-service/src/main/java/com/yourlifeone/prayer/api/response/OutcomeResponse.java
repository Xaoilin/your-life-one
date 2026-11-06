package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OutcomeResponse(
        UUID id,
        LocalDate date,
        PrayerName prayer,
        OutcomeStatus status,
        OutcomeTiming timing,
        Instant recordedAt,
        String source,
        String taskId,
        boolean rewarded,
        Instant deadlineAt) {
}
