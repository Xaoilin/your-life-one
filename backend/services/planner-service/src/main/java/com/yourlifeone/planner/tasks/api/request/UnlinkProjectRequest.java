package com.yourlifeone.planner.tasks.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The removed project whose tasks leave its board. */
public record UnlinkProjectRequest(@NotBlank @Size(max = 256) String projectId) {
}
