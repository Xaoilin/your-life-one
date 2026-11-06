package com.yourlifeone.life.inventory.domain;

/** A need marked acquired and the owned item its quantity went to. */
public record Acquisition(InventoryNeed need, InventoryItem item) {
}
