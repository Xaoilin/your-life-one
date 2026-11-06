package com.yourlifeone.knowledge.projects.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A catalogue section's projects in their new order; they must be exactly the section's projects.
 *
 * @param section {@code pinned}, {@code projects} or {@code archived}
 */
public record ReorderProjectsRequest(
        @NotBlank @Size(max = 16) String section,
        @NotNull @Size(max = 1000) List<@NotBlank @Size(max = 256) String> ids) {
}
