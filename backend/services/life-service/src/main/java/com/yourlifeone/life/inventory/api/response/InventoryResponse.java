package com.yourlifeone.life.inventory.api.response;

import java.util.List;

/** Items and needs: all of them for the app, or the matches of a search. */
public record InventoryResponse(List<InventoryItemResponse> items, List<InventoryNeedResponse> needs) {
}
