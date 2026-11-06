package com.yourlifeone.life.jobs.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * A job or freelance opportunity and its evidence history.
 *
 * @param applicationDate the local date it was submitted; null until it actually is
 * @param history evidence, oldest first
 */
@Builder(toBuilder = true)
public record JobApplication(
        String id,
        String company,
        String role,
        String url,
        String workType,
        String remoteRegion,
        String remoteStatus,
        String remoteEvidence,
        String remoteCaveat,
        String compensation,
        String status,
        LocalDate applicationDate,
        String nextAction,
        LocalDate nextActionDate,
        String notes,
        List<HistoryEntry> history,
        Instant createdAt,
        Instant updatedAt) {
}
