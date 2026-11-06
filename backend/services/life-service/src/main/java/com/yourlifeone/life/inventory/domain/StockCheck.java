package com.yourlifeone.life.inventory.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Whether enough of something is owned.
 *
 * @param unit the unit asked about; null accepts any unit
 * @param matches owned items whose name contains the query, most stock first
 */
public record StockCheck(String query, BigDecimal requiredQuantity, String unit, boolean sufficient,
        List<InventoryItem> matches) {
}
