package com.yourlifeone.life.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Something the user owns.
 *
 * @param subcategory detailed metadata inside the broad category; null when not set
 * @param imageUrl an HTTPS product image; the app shows an icon without one
 * @param specifications named details, in order (everything but the dimension envelope)
 * @param archivedAt set when the item is no longer owned; items are archived, never deleted
 */
@Builder(toBuilder = true)
public record InventoryItem(
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
        Instant lastVerifiedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {
}
