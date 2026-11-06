package com.yourlifeone.life.jobs.api.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record JobApplicationResponse(
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
        List<HistoryEntryResponse> history,
        Instant createdAt,
        Instant updatedAt) {
}
