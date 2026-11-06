package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * An item to create or enrich: with an existing item's ID only the fields given change; a new item
 * needs a name and a quantity, and the rest take Inventory's defaults.
 */
public record ItemCandidateRequest(
        @Size(max = 256) String id,
        @Size(min = 1, max = 160) String name,
        @Size(max = 40) String category,
        @Size(max = 40) String subcategory,
        @Size(max = 2048) String imageUrl,
        @Size(max = 32) String trackingMode,
        @DecimalMin("0") @DecimalMax("1000000000") BigDecimal quantity,
        @Size(min = 1, max = 32) String unit,
        @DecimalMin("0") @DecimalMax("1000000000") BigDecimal lowStockThreshold,
        @Size(max = 120) String brand,
        @Size(max = 120) String model,
        @Valid DimensionsRequest dimensions,
        Map<@Size(max = 60) String, @Size(max = 200) String> specifications,
        @Size(max = 32) String condition,
        @Size(max = 160) String location,
        List<@Size(max = 50) String> tags,
        @Size(max = 4000) String notes,
        List<@Size(max = 160) String> projectCatalogKeys,
        Instant lastVerifiedAt) {
}
