package com.yourlifeone.life.inventory.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InventoryItemResponse(
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
        DimensionsResponse dimensions,
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
