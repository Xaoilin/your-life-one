package com.yourlifeone.knowledge.lifestyle.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A column's items in their new order; they must be exactly the column's current items. */
public record ReorderColumnRequest(
        @NotBlank @Size(max = 32) String type,
        @NotNull @Size(max = 1000) List<@NotBlank @Size(max = 256) String> ids) {
}
