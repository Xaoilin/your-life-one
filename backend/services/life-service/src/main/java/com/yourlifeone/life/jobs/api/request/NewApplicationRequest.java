package com.yourlifeone.life.jobs.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * A new application. Omitted fields take the tracker's defaults (status lead, remote eligibility to be
 * verified); the ID is chosen by the service when absent.
 */
public record NewApplicationRequest(
        @Size(max = 256) String id,
        @NotBlank @Size(max = 256) String company,
        @NotBlank @Size(max = 256) String role,
        @Size(max = 2048) String url,
        @Size(max = 32) String workType,
        @Size(max = 32) String remoteRegion,
        @Size(max = 32) String remoteStatus,
        @Size(max = 10000) String remoteEvidence,
        @Size(max = 2000) String remoteCaveat,
        @Size(max = 2000) String compensation,
        @Size(max = 32) String status,
        LocalDate applicationDate,
        @Size(max = 2000) String nextAction,
        LocalDate nextActionDate,
        @Size(max = 10000) String notes,
        @Size(max = 1000) List<@Valid HistoryEntryRequest> history) {
}
