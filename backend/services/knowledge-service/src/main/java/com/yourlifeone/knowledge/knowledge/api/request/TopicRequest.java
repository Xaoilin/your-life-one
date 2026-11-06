package com.yourlifeone.knowledge.knowledge.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The whole topic; the ID is in the path.
 *
 * @param sortOrder position among the topics; absent on a new topic places it last, on an existing one keeps it
 */
public record TopicRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @Size(max = 40) String icon,
        @Size(max = 32) String color,
        @Min(0) Integer sortOrder) {
}
