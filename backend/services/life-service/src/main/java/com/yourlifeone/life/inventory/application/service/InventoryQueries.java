package com.yourlifeone.life.inventory.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.inventory.config.InventoryProperties;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.inventory.domain.InventorySearch;
import com.yourlifeone.life.inventory.domain.InventorySnapshot;
import com.yourlifeone.life.inventory.domain.StockCheck;
import com.yourlifeone.life.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryNeedRepository;
import com.yourlifeone.life.shared.AllowedValues;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Reads Inventory: everything for the app; searches and stock checks for agents. */
@Service
@RequiredArgsConstructor
public class InventoryQueries {
    private static final int MAX_TEXT = 160;
    private static final BigDecimal MAX_QUANTITY = BigDecimal.valueOf(1_000_000_000);

    private final InventoryItemRepository items;
    private final InventoryNeedRepository needs;
    private final InventoryPersistenceMapper persistence;
    private final InventoryProperties properties;

    @Transactional(readOnly = true)
    public InventorySnapshot all(UUID userId) {
        return new InventorySnapshot(allItems(userId), allNeeds(userId));
    }

    /** Owned (not archived) items and open (needed or ordered) needs that match, by name. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public InventorySnapshot search(UUID userId, InventorySearch search) {
        requireBounds(search);
        var needle = search.query().trim().toLowerCase(Locale.ROOT);
        var itemIds = items.searchIds(userId, needle, search.projectCatalogKey(), search.category(),
                search.location(), search.limit());
        var needIds = needs.searchIds(userId, needle, search.projectCatalogKey(), search.limit());
        var matchingItems = selectedItems(userId, itemIds);
        var matchingNeeds = selectedNeeds(userId, needIds);
        return new InventorySnapshot(matchingItems, matchingNeeds);
    }

    /** Enough stock when an owned item whose name contains the query has the quantity (in the unit, if given). */
    @Transactional(readOnly = true)
    public StockCheck check(UUID userId, String name, BigDecimal requiredQuantity, String unit) {
        if (name == null || name.isBlank() || name.length() > MAX_TEXT || requiredQuantity.signum() < 0
                || requiredQuantity.compareTo(MAX_QUANTITY) > 0 || (unit != null && unit.length() > 32)) {
            throw ApiException.badRequest("invalid_check", "Name, quantity or unit is out of bounds.");
        }
        var needle = name.trim().toLowerCase(Locale.ROOT);
        var matches = allItems(userId).stream()
                .filter(item -> item.archivedAt() == null)
                .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(InventoryItem::quantity).reversed())
                .toList();
        var sufficient = matches.stream().anyMatch(item -> item.quantity().compareTo(requiredQuantity) >= 0
                && (unit == null || item.unit().equalsIgnoreCase(unit.trim())));
        return new StockCheck(name.trim(), requiredQuantity, unit, sufficient, matches);
    }

    private List<InventoryItem> allItems(UUID userId) {
        return items.findByUserIdOrderByName(userId).stream().map(persistence::toDomain).toList();
    }

    private List<InventoryNeed> allNeeds(UUID userId) {
        return needs.findByUserIdOrderByName(userId).stream().map(persistence::toDomain).toList();
    }

    private void requireBounds(InventorySearch search) {
        if (search.query().length() > MAX_TEXT || search.limit() < 1 || search.limit() > properties.maxSearchResults()
                || (search.projectCatalogKey() != null && search.projectCatalogKey().length() > MAX_TEXT)
                || (search.location() != null && search.location().length() > MAX_TEXT)) {
            throw ApiException.badRequest("invalid_search", "Search query, filters or limit are out of bounds.");
        }
        AllowedValues.optional("category", search.category(), properties.categories());
    }

    private List<InventoryItem> selectedItems(UUID userId, List<String> ids) {
        if (ids.isEmpty()) return List.of();
        var selected = items.fetchSelected(userId, ids).stream().map(persistence::toDomain)
                .collect(Collectors.toMap(InventoryItem::id, item -> item));
        return ids.stream().map(selected::get).toList();
    }

    private List<InventoryNeed> selectedNeeds(UUID userId, List<String> ids) {
        if (ids.isEmpty()) return List.of();
        var selected = needs.fetchSelected(userId, ids).stream().map(persistence::toDomain)
                .collect(Collectors.toMap(InventoryNeed::id, need -> need));
        return ids.stream().map(selected::get).toList();
    }
}
