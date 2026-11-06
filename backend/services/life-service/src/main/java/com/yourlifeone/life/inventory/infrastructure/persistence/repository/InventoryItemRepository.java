package com.yourlifeone.life.inventory.infrastructure.persistence.repository;

import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.yourlifeone.life.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, UserScopedId> {
    List<InventoryItemEntity> findByUserIdOrderByName(UUID userId);

    /** Limit IDs before fetching the selected parents and their complete ordered specifications. */
    @Query(value = """
            SELECT i.id FROM inventory.item i
            WHERE i.user_id = :userId AND i.archived_at IS NULL
              AND (:project IS NULL OR :project = ANY(i.project_catalog_keys))
              AND (:category IS NULL OR i.category = :category)
              AND (:location IS NULL OR i.location = :location)
              AND (:needle = '' OR strpos(lower(concat_ws(E'\\n',
                i.name,i.category,i.subcategory,i.unit,i.brand,i.model,i.location,i.notes,
                nullif(array_to_string(i.tags,E'\\n'),''),
                nullif(array_to_string(i.project_catalog_keys,E'\\n'),''),
                (SELECT string_agg(s.name,E'\\n' ORDER BY s.position) || E'\\n' ||
                    string_agg(s.value,E'\\n' ORDER BY s.position)
                 FROM inventory.item_specification s WHERE s.user_id=i.user_id AND s.item_id=i.id)
              ) COLLATE "und-x-icu"), :needle) > 0)
            ORDER BY lower(i.name COLLATE "und-x-icu") COLLATE "C", i.name, i.id LIMIT :limit
            """, nativeQuery = true)
    List<String> searchIds(UUID userId, String needle, String project, String category, String location, int limit);

    @Query("SELECT i FROM InventoryItemEntity i LEFT JOIN FETCH i.specifications "
            + "WHERE i.userId = :userId AND i.id IN :ids")
    List<InventoryItemEntity> fetchSelected(UUID userId, List<String> ids);
}
