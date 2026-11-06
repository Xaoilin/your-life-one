package com.yourlifeone.life.jobs.api.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Changes to one application: set fields replace stored values, {@code clear} removes optional fields
 * (see {@code app.life.jobs.clearable-fields}); history is appended through its own endpoint.
 *
 * @param expectedUpdatedAt the version the caller last saw; a newer stored version refuses the change
 */
public record ApplicationPatchRequest(
        @Size(min = 1, max = 256) String company,
        @Size(min = 1, max = 256) String role,
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
        @Size(max = 20) List<@Size(max = 64) String> clear,
        Instant expectedUpdatedAt) {
}
