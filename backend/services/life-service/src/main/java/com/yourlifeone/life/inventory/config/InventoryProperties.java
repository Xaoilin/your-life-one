package com.yourlifeone.life.inventory.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed values and limits of Inventory ({@code app.life.inventory.*}).
 *
 * @param subcategories each detailed subcategory and the broad category it belongs to; the categories
 *                      are exactly the values of this map
 * @param maxBatch most items one save may create or change
 */
@ConfigurationProperties("app.life.inventory")
public record InventoryProperties(
        List<String> trackingModes,
        List<String> conditions,
        List<String> priorities,
        List<String> needStatuses,
        List<String> dimensionUnits,
        Map<String, String> subcategories,
        int maxBatch,
        int maxSearchResults,
        int maxSpecifications,
        int maxTags,
        int maxProjectKeys) {
    public InventoryProperties {
        trackingModes = List.copyOf(trackingModes);
        conditions = List.copyOf(conditions);
        priorities = List.copyOf(priorities);
        needStatuses = List.copyOf(needStatuses);
        dimensionUnits = List.copyOf(dimensionUnits);
        subcategories = Map.copyOf(subcategories);
    }

    public List<String> categories() {
        return List.copyOf(new LinkedHashSet<>(subcategories.values()));
    }
}
