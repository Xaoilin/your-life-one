package com.yourlifeone.life.inventory.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record InventoryNeedResponse(
        String id,
        String name,
        String category,
        String subcategory,
        String imageUrl,
        String linkedItemId,
        String projectCatalogKey,
        BigDecimal requiredQuantity,
        String unit,
        DimensionsResponse dimensions,
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
