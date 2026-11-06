package com.yourlifeone.life.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * An item to create or enrich. With the ID of an existing item only the fields given change; otherwise
 * a new item is created and needs at least a name and a quantity.
 */
@Builder(toBuilder = true)
public record ItemCandidate(
        String id,
        String name,
        String category,
        String subcategory,
        String imageUrl,
        String trackingMode,
        BigDecimal quantity,
        String unit,
        BigDecimal lowStockThreshold,
        String brand,
        String model,
        Dimensions dimensions,
        Map<String, String> specifications,
        String condition,
        String location,
        List<String> tags,
        String notes,
        List<String> projectCatalogKeys,
        Instant lastVerifiedAt) {
}
