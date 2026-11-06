package com.yourlifeone.knowledge.lifestyle.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.lifestyle.domain.LifestyleItem;
import com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.mapper.LifestylePersistenceMapper;
import com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.repository.LifestyleItemRepository;
import com.yourlifeone.knowledge.shared.RecordIds;
import com.yourlifeone.knowledge.shared.Texts;
import com.yourlifeone.knowledge.shared.Timestamps;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The lifestyle tracker. Saves and deletes name one item; moves and reorders renumber the columns they
 * touch from the stored items, never from the caller's copy, so nothing is lost when copies disagree.
 */
@Service
@RequiredArgsConstructor
public class LifestyleService {
    private final LifestyleItemRepository items;
    private final LifestylePersistenceMapper persistence;
    private final LifestyleRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<LifestyleItem> list(UUID userId) {
        return load(userId);
    }

    /**
     * Creates the item at the end of its column, or replaces its content. Changing the type moves it to the
     * end of the new column.
     */
    @Transactional
    public LifestyleItem save(UUID userId, LifestyleItem requested) {
        var id = RecordIds.require(requested.id());
        var type = rules.requireType(requested.type());
        var all = load(userId);
        var existing = all.stream().filter(item -> item.id().equals(id)).findFirst();
        var now = Timestamps.now(clock);
        var item = requested.toBuilder()
                .type(type)
                .title(requested.title().trim())
                .notes(Texts.trimmed(requested.notes()))
                .status(rules.requireStatus(type, requested.status()))
                .sources(rules.sources(Texts.distinctTrimmed(requested.sources())))
                .sortOrder(existing.map(LifestyleItem::sortOrder).orElse(null))
                .createdAt(existing.map(LifestyleItem::createdAt).orElse(now))
                .updatedAt(now)
                .build();
        if (existing.isPresent() && existing.get().type().equals(type)) {
            return store(userId, item);
        }
        var others = all.stream().filter(other -> !other.id().equals(id)).toList();
        saveAll(userId, LifestyleColumns.insert(LifestyleColumns.column(others, type), item, null, now));
        existing.ifPresent(previous -> saveAll(userId,
                LifestyleColumns.renumber(LifestyleColumns.column(others, previous.type()), now)));
        return find(userId, id);
    }

    /**
     * Moves an item to a position in a column (the end when absent). Moving between the avoid and practise
     * families restarts its status. Returns every item.
     */
    @Transactional
    public List<LifestyleItem> move(UUID userId, String id, String toType, Integer position) {
        var type = rules.requireType(toType);
        var all = load(userId);
        var item = all.stream().filter(candidate -> candidate.id().equals(RecordIds.require(id))).findFirst()
                .orElseThrow(LifestyleService::notFound);
        var now = Timestamps.now(clock);
        var moved = item.toBuilder().type(type).status(rules.statusAfterMove(item.type(), type, item.status()))
                .updatedAt(now).build();
        var others = all.stream().filter(other -> !other.id().equals(id)).toList();
        saveAll(userId, LifestyleColumns.insert(LifestyleColumns.column(others, type), moved, position, now));
        if (!item.type().equals(type)) {
            saveAll(userId, LifestyleColumns.renumber(LifestyleColumns.column(others, item.type()), now));
        }
        return load(userId);
    }

    /**
     * Puts a column in the given order. The IDs must be exactly the column's items; otherwise the caller's
     * view is out of date and nothing changes. Returns every item.
     */
    @Transactional
    public List<LifestyleItem> reorder(UUID userId, String type, List<String> orderedIds) {
        rules.requireType(type);
        var column = LifestyleColumns.column(load(userId), type);
        var current = column.stream().map(LifestyleItem::id).toList();
        if (orderedIds.size() != current.size() || !new HashSet<>(orderedIds).equals(new HashSet<>(current))) {
            throw ApiException.conflict("lifestyle_order_changed",
                    "The column changed since it was shown; reload and try again.");
        }
        var byId = column.stream().collect(Collectors.toMap(LifestyleItem::id, Function.identity()));
        saveAll(userId, LifestyleColumns.renumber(orderedIds.stream().map(byId::get).toList(), Timestamps.now(clock)));
        return load(userId);
    }

    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!items.existsById(key)) throw notFound();
        items.deleteById(key);
    }

    private List<LifestyleItem> load(UUID userId) {
        return items.findByUserIdOrderBySortOrderAscCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain)
                .toList();
    }

    private LifestyleItem find(UUID userId, String id) {
        return items.findById(new UserScopedId(userId, id)).map(persistence::toDomain).orElseThrow(LifestyleService::notFound);
    }

    private LifestyleItem store(UUID userId, LifestyleItem item) {
        return persistence.toDomain(items.save(persistence.toEntity(userId, item)));
    }

    private void saveAll(UUID userId, List<LifestyleItem> changed) {
        items.saveAll(changed.stream().map(item -> persistence.toEntity(userId, item)).toList());
    }

    private static ApiException notFound() {
        return ApiException.notFound("item_not_found", "This lifestyle item does not exist.");
    }
}
