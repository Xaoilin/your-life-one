package com.yourlifeone.knowledge.projects.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new status for a project that is not archived (archiving has its own operation). */
public record ProjectStatusRequest(@NotBlank @Size(max = 32) String status) {
}
