package com.yourlifeone.life.inventory.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.inventory.domain.Acquisition;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryNeedEntity;
import com.yourlifeone.life.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import com.yourlifeone.life.inventory.infrastructure.persistence.repository.InventoryNeedRepository;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Need writes. Status changes stamp their time once; acquiring a need adds its quantity to the linked
 * item (or a new one) in the same transaction, so stock and the shopping list never disagree.
 */
@Service
@RequiredArgsConstructor
public class InventoryNeedService {
    private static final String ORDERED = "ordered";
    private static final String ACQUIRED = "acquired";
    private static final String DISMISSED = "dismissed";

    private final InventoryNeedRepository needs;
    private final InventoryItemRepository items;
    private final InventoryItemService itemService;
    private final InventoryPersistenceMapper persistence;
    private final InventoryRules rules;
    private final Clock clock;

    /** Creates the need, or replaces it keeping its creation and status times. */
    @Transactional
    public InventoryNeed save(UUID userId, InventoryNeed requested) {
        var existing = needs.findById(new UserScopedId(userId, RecordIds.require(requested.id())));
        var current = existing.map(persistence::toDomain).orElse(null);
        if (ACQUIRED.equals(requested.status()) && (current == null || !ACQUIRED.equals(current.status()))) {
            throw ApiException.badRequest("acquire_through_stock", "Mark a need acquired with /needs/{id}/acquire.");
        }
        var now = Timestamps.now(clock);
        var need = rules.normalized(requested.toBuilder()
                .orderedAt(stamp(current == null ? null : current.orderedAt(), ORDERED.equals(requested.status()), now))
                .acquiredAt(current == null ? null : current.acquiredAt())
                .dismissedAt(stamp(current == null ? null : current.dismissedAt(), DISMISSED.equals(requested.status()), now))
                .createdAt(current == null ? now : current.createdAt())
                .updatedAt(now)
                .build());
        return store(userId, existing.orElse(null), need);
    }

    /**
     * Closes the need and adds its quantity to the linked owned item when it has one in the same unit,
     * otherwise to a new item made from the need. Acquiring an acquired need changes nothing.
     */
    @Transactional
    public Acquisition acquire(UUID userId, String needId, String newItemId) {
        var entity = needs.findById(new UserScopedId(userId, RecordIds.require(needId)))
                .orElseThrow(() -> ApiException.notFound("need_not_found", "This need does not exist."));
        var need = persistence.toDomain(entity);
        if (ACQUIRED.equals(need.status())) {
            var linked = need.linkedItemId() == null ? null : findItem(userId, need.linkedItemId());
            return new Acquisition(need, linked);
        }
        var now = Timestamps.now(clock);
        var linked = need.linkedItemId() == null ? null : findItem(userId, need.linkedItemId());
        InventoryItem item;
        if (linked != null && linked.archivedAt() == null) {
            item = itemService.save(userId, addStock(linked, need, now));
        } else {
            var id = newItemId == null ? UUID.randomUUID().toString() : RecordIds.require(newItemId);
            item = itemService.save(userId, itemFrom(need, id, now));
        }
        var acquired = need.toBuilder().status(ACQUIRED).acquiredAt(now).linkedItemId(item.id()).updatedAt(now).build();
        return new Acquisition(store(userId, entity, acquired), item);
    }

    private InventoryItem findItem(UUID userId, String id) {
        return items.findById(new UserScopedId(userId, id)).map(persistence::toDomain).orElse(null);
    }

    private static InventoryItem addStock(InventoryItem linked, InventoryNeed need, Instant now) {
        if (!linked.unit().trim().toLowerCase(Locale.ROOT).equals(need.unit().trim().toLowerCase(Locale.ROOT))) {
            throw ApiException.conflict("unit_mismatch",
                    "The linked item and need must use the same unit before acquisition.");
        }
        return linked.toBuilder().quantity(linked.quantity().add(need.requiredQuantity())).lastVerifiedAt(now).build();
    }

    private static InventoryItem itemFrom(InventoryNeed need, String id, Instant now) {
        return InventoryItem.builder()
                .id(id)
                .name(need.name())
                .category(Objects.requireNonNullElse(need.category(), "other"))
                .subcategory(need.subcategory())
                .imageUrl(need.imageUrl())
                .trackingMode("counted")
                .quantity(need.requiredQuantity())
                .unit(need.unit())
                .dimensions(need.dimensions())
                .specifications(need.specifications())
                .condition("new")
                .tags(List.of())
                .notes(need.notes())
                .projectCatalogKeys(need.projectCatalogKey() == null ? List.of() : List.of(need.projectCatalogKey()))
                .lastVerifiedAt(now)
                .build();
    }

    private InventoryNeed store(UUID userId, InventoryNeedEntity existing, InventoryNeed need) {
        if (existing == null) {
            return persistence.toDomain(needs.save(persistence.toEntity(userId, need)));
        }
        persistence.update(existing, need);
        return persistence.toDomain(needs.save(existing));
    }

    /** Keeps the first time a status was reached; stamps now when it is reached for the first time. */
    private static Instant stamp(Instant previous, boolean reached, Instant now) {
        if (previous != null) return previous;
        return reached ? now : null;
    }
}
