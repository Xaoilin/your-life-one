package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The whole need; the ID is in the path. Moving to {@code ordered} or {@code dismissed} stamps when;
 * acquiring goes through {@code /needs/{id}/acquire} so the stock changes with it.
 */
public record NeedRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 40) String category,
        @Size(max = 40) String subcategory,
        @Size(max = 2048) String imageUrl,
        @Size(max = 256) String linkedItemId,
        @Size(max = 160) String projectCatalogKey,
        @NotNull @DecimalMin("0") @DecimalMax("1000000000") BigDecimal requiredQuantity,
        @NotBlank @Size(max = 32) String unit,
        @Valid DimensionsRequest dimensions,
        Map<@Size(max = 60) String, @Size(max = 200) String> specifications,
        @NotNull @Size(max = 32) String priority,
        @NotNull @Size(max = 32) String status,
        @Size(max = 4000) String notes) {
}
