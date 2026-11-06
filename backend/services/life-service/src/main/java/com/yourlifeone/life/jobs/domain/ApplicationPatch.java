package com.yourlifeone.life.jobs.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Changes to one application: a non-null field replaces the stored value, {@code clear} names optional
 * fields to remove, and everything else (including the history) is kept.
 *
 * @param expectedUpdatedAt when set, the change is refused unless the application is still this version
 */
@Builder
public record ApplicationPatch(
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
        List<String> clear,
        Instant expectedUpdatedAt) {
}
