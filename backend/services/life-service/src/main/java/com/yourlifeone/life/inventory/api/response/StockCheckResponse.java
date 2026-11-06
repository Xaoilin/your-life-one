package com.yourlifeone.life.inventory.api.response;

import java.math.BigDecimal;
import java.util.List;

public record StockCheckResponse(String query, BigDecimal requiredQuantity, String unit, boolean sufficient,
        List<InventoryItemResponse> matches) {
}
