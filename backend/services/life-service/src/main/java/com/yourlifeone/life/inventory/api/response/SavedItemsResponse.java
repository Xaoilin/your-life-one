package com.yourlifeone.life.inventory.api.response;

import java.util.List;

/** The items as stored after a save, in the order they were given. */
public record SavedItemsResponse(List<InventoryItemResponse> items) {
}
