package com.yourlifeone.life.inventory.domain;

import java.util.List;

public record InventorySnapshot(List<InventoryItem> items, List<InventoryNeed> needs) {
}
