package com.yourlifeone.life.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

/**
 * Something to get: needed, then ordered, then acquired (which adds it to stock) or dismissed.
 *
 * @param linkedItemId the owned item acquiring this need adds to; null to create a new item
 */
@Builder(toBuilder = true)
public record InventoryNeed(
        String id,
        String name,
        String category,
        String subcategory,
        String imageUrl,
        String linkedItemId,
        String projectCatalogKey,
        BigDecimal requiredQuantity,
        String unit,
        Dimensions dimensions,
        Map<String, String> specifications,
        String priority,
        String status,
        String notes,
        Instant orderedAt,
        Instant acquiredAt,
        Instant dismissedAt,
        Instant createdAt,
        Instant updatedAt) {
}
