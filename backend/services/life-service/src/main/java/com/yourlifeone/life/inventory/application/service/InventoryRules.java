package com.yourlifeone.life.inventory.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.inventory.config.InventoryProperties;
import com.yourlifeone.life.inventory.domain.Dimensions;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.shared.AllowedValues;
import com.yourlifeone.life.shared.RecordIds;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Inventory's rules: allowed values, a subcategory inside its category, HTTPS images only, an
 * optional dimension envelope, and bounded specifications, tags and project links. Normalising trims
 * text and drops blank optional values so the stored record is what the app shows.
 */
@Component
@RequiredArgsConstructor
public class InventoryRules {
    private static final Pattern HTTPS_IMAGE = Pattern.compile("^https://[^/@:\\s]+(:\\d{1,5})?([/?#]\\S*)?$");
    private static final int MAX_SPEC_NAME = 60;
    private static final int MAX_SPEC_VALUE = 200;

    private final InventoryProperties properties;

    public InventoryItem normalized(InventoryItem item) {
        var normalized = item.toBuilder()
                .name(item.name().trim())
                .subcategory(blankToNull(item.subcategory()))
                .imageUrl(blankToNull(item.imageUrl()))
                .unit(item.unit().trim())
                .brand(blankToNull(item.brand()))
                .model(blankToNull(item.model()))
                .specifications(trimmedSpecifications(item.specifications()))
                .location(blankToNull(item.location()))
                .tags(trimmedList("tags", item.tags(), properties.maxTags()))
                .notes(item.notes() == null ? "" : item.notes())
                .projectCatalogKeys(trimmedList("projectCatalogKeys", item.projectCatalogKeys(),
                        properties.maxProjectKeys()))
                .build();
        RecordIds.require(normalized.id());
        var category = AllowedValues.require("category", normalized.category(), properties.categories());
        requireSubcategory(category, normalized.subcategory());
        AllowedValues.require("trackingMode", normalized.trackingMode(), properties.trackingModes());
        AllowedValues.require("condition", normalized.condition(), properties.conditions());
        requireImage(normalized.imageUrl());
        requireDimensions(normalized.dimensions());
        return normalized;
    }

    public InventoryNeed normalized(InventoryNeed need) {
        var normalized = need.toBuilder()
                .name(need.name().trim())
                .category(blankToNull(need.category()))
                .subcategory(blankToNull(need.subcategory()))
                .imageUrl(blankToNull(need.imageUrl()))
                .linkedItemId(blankToNull(need.linkedItemId()))
                .projectCatalogKey(blankToNull(need.projectCatalogKey()))
                .unit(need.unit().trim())
                .specifications(trimmedSpecifications(need.specifications()))
                .notes(need.notes() == null ? "" : need.notes())
                .build();
        RecordIds.require(normalized.id());
        AllowedValues.optional("category", normalized.category(), properties.categories());
        if (normalized.subcategory() != null && normalized.category() == null) {
            throw ApiException.badRequest("invalid_subcategory", "A subcategory needs its category.");
        }
        requireSubcategory(normalized.category(), normalized.subcategory());
        AllowedValues.require("priority", normalized.priority(), properties.priorities());
        AllowedValues.require("status", normalized.status(), properties.needStatuses());
        requireImage(normalized.imageUrl());
        requireDimensions(normalized.dimensions());
        return normalized;
    }

    private void requireSubcategory(String category, String subcategory) {
        if (subcategory == null) return;
        var owner = properties.subcategories().get(subcategory);
        if (owner == null || !owner.equals(category)) {
            throw ApiException.badRequest("invalid_subcategory",
                    "subcategory " + subcategory + " does not belong to category " + category + ".");
        }
    }

    private static void requireImage(String imageUrl) {
        if (imageUrl != null && !HTTPS_IMAGE.matcher(imageUrl).matches()) {
            throw ApiException.badRequest("invalid_image_url", "Product image URL must be a valid HTTPS address.");
        }
    }

    private void requireDimensions(Dimensions dimensions) {
        if (dimensions == null) return;
        AllowedValues.require("dimensionUnit", dimensions.unit(), properties.dimensionUnits());
        if (dimensions.length() == null && dimensions.width() == null && dimensions.height() == null) {
            throw ApiException.badRequest("invalid_dimensions", "Dimensions need at least one positive axis.");
        }
    }

    private Map<String, String> trimmedSpecifications(Map<String, String> specifications) {
        var trimmed = new LinkedHashMap<String, String>();
        if (specifications == null) return trimmed;
        specifications.forEach((name, value) -> {
            if (name == null || value == null || name.isBlank() || value.isBlank()
                    || name.trim().length() > MAX_SPEC_NAME || value.trim().length() > MAX_SPEC_VALUE) {
                throw ApiException.badRequest("invalid_specifications",
                        "Specifications are 1-60 character names with 1-200 character values.");
            }
            if (trimmed.put(name.trim(), value.trim()) != null) {
                throw ApiException.badRequest("invalid_specifications", "Specification \"" + name.trim()
                        + "\" appears more than once.");
            }
        });
        if (trimmed.size() > properties.maxSpecifications()) {
            throw ApiException.badRequest("invalid_specifications",
                    "At most " + properties.maxSpecifications() + " specifications are allowed.");
        }
        return trimmed;
    }

    /** Trims, drops blanks and repeats, keeping the first order. */
    private static List<String> trimmedList(String field, List<String> values, int max) {
        if (values == null) return List.of();
        var trimmed = new LinkedHashSet<String>();
        values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).forEach(trimmed::add);
        if (trimmed.size() > max) {
            throw ApiException.badRequest("invalid_" + field, "At most " + max + " " + field + " are allowed.");
        }
        return List.copyOf(trimmed);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
