package com.yourlifeone.life.inventory.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.inventory.config.InventoryProperties;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.ItemCandidate;
import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.yourlifeone.life.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.shared.Values;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owned-item writes. Items are never deleted: an item no longer owned is archived. */
@Service
@RequiredArgsConstructor
public class InventoryItemService {
    private static final String DEFAULT_CATEGORY = "other";
    private static final String DEFAULT_TRACKING = "counted";
    private static final String DEFAULT_UNIT = "units";
    private static final String DEFAULT_CONDITION = "unknown";

    private final InventoryItemRepository repository;
    private final InventoryPersistenceMapper persistence;
    private final InventoryRules rules;
    private final InventoryProperties properties;
    private final Clock clock;

    /** Creates the item, or replaces it keeping its creation time. */
    @Transactional
    public InventoryItem save(UUID userId, InventoryItem requested) {
        var existing = repository.findById(new UserScopedId(userId, RecordIds.require(requested.id())));
        var now = Timestamps.now(clock);
        var item = rules.normalized(requested.toBuilder()
                .lastVerifiedAt(Objects.requireNonNullElseGet(requested.lastVerifiedAt(),
                        () -> existing.map(InventoryItemEntity::getLastVerifiedAt).orElse(now)))
                .createdAt(existing.map(InventoryItemEntity::getCreatedAt).orElse(now))
                .updatedAt(now)
                .build());
        return store(userId, existing.orElse(null), item);
    }

    /**
     * Creates or enriches every candidate, all or none: an existing ID changes only the fields given,
     * a new candidate needs a name and quantity and takes the defaults for the rest.
     */
    @Transactional
    public List<InventoryItem> saveAll(UUID userId, List<ItemCandidate> candidates) {
        if (candidates.size() > properties.maxBatch()) {
            throw ApiException.badRequest("batch_too_large", "At most " + properties.maxBatch() + " items per save.");
        }
        var ids = new HashSet<String>();
        var now = Timestamps.now(clock);
        return candidates.stream().map(candidate -> {
            var id = candidate.id() == null ? UUID.randomUUID().toString() : RecordIds.require(candidate.id());
            if (!ids.add(id)) throw ApiException.badRequest("duplicate_item", "Item " + id + " appears twice.");
            var existing = repository.findById(new UserScopedId(userId, id));
            var item = existing.map(entity -> enrich(persistence.toDomain(entity), candidate, now))
                    .orElseGet(() -> create(id, candidate, now));
            return store(userId, existing.orElse(null), rules.normalized(item));
        }).toList();
    }

    /** Archiving an archived item keeps its first archive time. */
    @Transactional
    public InventoryItem archive(UUID userId, String id) {
        var entity = repository.findById(new UserScopedId(userId, RecordIds.require(id)))
                .orElseThrow(() -> ApiException.notFound("item_not_found", "This item does not exist."));
        if (entity.getArchivedAt() != null) return persistence.toDomain(entity);
        var now = Timestamps.now(clock);
        var item = persistence.toDomain(entity).toBuilder().archivedAt(now).updatedAt(now).build();
        return store(userId, entity, item);
    }

    private InventoryItem store(UUID userId, InventoryItemEntity existing, InventoryItem item) {
        if (existing == null) {
            return persistence.toDomain(repository.save(persistence.toEntity(userId, item)));
        }
        persistence.update(existing, item);
        return persistence.toDomain(repository.save(existing));
    }

    private static InventoryItem enrich(InventoryItem current, ItemCandidate candidate, Instant now) {
        return current.toBuilder()
                .name(Values.orElse(candidate.name(), current.name()))
                .category(Values.orElse(candidate.category(), current.category()))
                .subcategory(Values.orElse(candidate.subcategory(), nullIfCategoryChanged(current, candidate)))
                .imageUrl(Values.orElse(candidate.imageUrl(), current.imageUrl()))
                .trackingMode(Values.orElse(candidate.trackingMode(), current.trackingMode()))
                .quantity(Values.orElse(candidate.quantity(), current.quantity()))
                .unit(Values.orElse(candidate.unit(), current.unit()))
                .lowStockThreshold(Values.orElse(candidate.lowStockThreshold(), current.lowStockThreshold()))
                .brand(Values.orElse(candidate.brand(), current.brand()))
                .model(Values.orElse(candidate.model(), current.model()))
                .dimensions(Values.orElse(candidate.dimensions(), current.dimensions()))
                .specifications(Values.orElse(candidate.specifications(), current.specifications()))
                .condition(Values.orElse(candidate.condition(), current.condition()))
                .location(Values.orElse(candidate.location(), current.location()))
                .tags(Values.orElse(candidate.tags(), current.tags()))
                .notes(Values.orElse(candidate.notes(), current.notes()))
                .projectCatalogKeys(Values.orElse(candidate.projectCatalogKeys(), current.projectCatalogKeys()))
                .lastVerifiedAt(Values.orElse(candidate.lastVerifiedAt(), current.lastVerifiedAt()))
                .updatedAt(now)
                .build();
    }

    /** A new category without a new subcategory drops the old subcategory, which belonged to the old category. */
    private static String nullIfCategoryChanged(InventoryItem current, ItemCandidate candidate) {
        return candidate.category() == null || candidate.category().equals(current.category())
                ? current.subcategory() : null;
    }

    private static InventoryItem create(String id, ItemCandidate candidate, Instant now) {
        if (candidate.name() == null || candidate.quantity() == null) {
            throw ApiException.badRequest("incomplete_item", "New Inventory items require a name and quantity.");
        }
        return InventoryItem.builder()
                .id(id)
                .name(candidate.name())
                .category(Objects.requireNonNullElse(candidate.category(), DEFAULT_CATEGORY))
                .subcategory(candidate.subcategory())
                .imageUrl(candidate.imageUrl())
                .trackingMode(Objects.requireNonNullElse(candidate.trackingMode(), DEFAULT_TRACKING))
                .quantity(candidate.quantity())
                .unit(Objects.requireNonNullElse(candidate.unit(), DEFAULT_UNIT))
                .lowStockThreshold(candidate.lowStockThreshold())
                .brand(candidate.brand())
                .model(candidate.model())
                .dimensions(candidate.dimensions())
                .specifications(Objects.requireNonNullElse(candidate.specifications(), Map.of()))
                .condition(Objects.requireNonNullElse(candidate.condition(), DEFAULT_CONDITION))
                .location(candidate.location())
                .tags(Objects.requireNonNullElse(candidate.tags(), List.of()))
                .notes(Objects.requireNonNullElse(candidate.notes(), ""))
                .projectCatalogKeys(Objects.requireNonNullElse(candidate.projectCatalogKeys(), List.of()))
                .lastVerifiedAt(Objects.requireNonNullElse(candidate.lastVerifiedAt(), now))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
