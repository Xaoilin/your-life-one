package com.yourlifeone.life.inventory.domain;

import lombok.Builder;

/**
 * What an agent looks for among owned items and open needs.
 *
 * @param query case-insensitive text anywhere in a record; blank matches everything
 */
@Builder
public record InventorySearch(String query, String projectCatalogKey, String category, String location, int limit) {
}
