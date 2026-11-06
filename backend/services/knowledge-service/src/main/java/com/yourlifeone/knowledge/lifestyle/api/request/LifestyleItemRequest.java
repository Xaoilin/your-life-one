package com.yourlifeone.knowledge.lifestyle.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The whole item; the ID is in the path. The status must belong to the type's family; a new item goes to
 * the end of its column, and a changed type moves it to the end of the new one.
 */
public record LifestyleItemRequest(
        @NotBlank @Size(max = 32) String type,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 4000) String notes,
        @NotBlank @Size(max = 32) String status,
        @NotNull List<@NotNull @Size(max = 2048) String> sources) {
}
