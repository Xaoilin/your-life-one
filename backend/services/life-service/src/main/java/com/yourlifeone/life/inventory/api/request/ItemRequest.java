package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The whole item; the ID is in the path. Allowed values are checked against configuration.
 *
 * @param lastVerifiedAt when the stock was last checked; null keeps the stored time (now for a new item)
 * @param archivedAt set when the item is no longer owned
 */
public record ItemRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull @Size(max = 40) String category,
        @Size(max = 40) String subcategory,
        @Size(max = 2048) String imageUrl,
        @NotNull @Size(max = 32) String trackingMode,
        @NotNull @DecimalMin("0") @DecimalMax("1000000000") BigDecimal quantity,
        @NotBlank @Size(max = 32) String unit,
        @DecimalMin("0") @DecimalMax("1000000000") BigDecimal lowStockThreshold,
        @Size(max = 120) String brand,
        @Size(max = 120) String model,
        @Valid DimensionsRequest dimensions,
        Map<@Size(max = 60) String, @Size(max = 200) String> specifications,
        @NotNull @Size(max = 32) String condition,
        @Size(max = 160) String location,
        List<@NotNull @Size(max = 50) String> tags,
        @Size(max = 4000) String notes,
        List<@NotNull @Size(max = 160) String> projectCatalogKeys,
        Instant lastVerifiedAt,
        Instant archivedAt) {
}
