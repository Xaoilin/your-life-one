package com.yourlifeone.knowledge.projects.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The whole page; the ID is in the path. A blank title becomes the default page title.
 *
 * @param isOverview makes this the project's overview page (the previous one becomes a normal page)
 */
public record PageRequest(
        @NotBlank @Size(max = 256) String projectId,
        @Size(max = 200) String title,
        @Size(max = 200000) String content,
        boolean isOverview) {
}
