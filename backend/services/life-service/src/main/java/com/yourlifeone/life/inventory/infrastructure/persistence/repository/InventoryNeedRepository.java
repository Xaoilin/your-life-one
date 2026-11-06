package com.yourlifeone.life.inventory.infrastructure.persistence.repository;

import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryNeedEntity;
import com.yourlifeone.life.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryNeedRepository extends JpaRepository<InventoryNeedEntity, UserScopedId> {
    List<InventoryNeedEntity> findByUserIdOrderByName(UUID userId);

    @Query(value = """
            SELECT n.id FROM inventory.need n
            WHERE n.user_id = :userId AND n.status IN ('needed','ordered')
              AND (:project IS NULL OR n.project_catalog_key = :project)
              AND (:needle = '' OR strpos(lower(concat_ws(E'\\n',
                n.name,n.category,n.subcategory,n.unit,n.notes,n.project_catalog_key,n.status,n.priority,
                (SELECT string_agg(s.name,E'\\n' ORDER BY s.position) || E'\\n' ||
                    string_agg(s.value,E'\\n' ORDER BY s.position)
                 FROM inventory.need_specification s WHERE s.user_id=n.user_id AND s.need_id=n.id)
              ) COLLATE "und-x-icu"), :needle) > 0)
            ORDER BY lower(n.name COLLATE "und-x-icu") COLLATE "C", n.name, n.id LIMIT :limit
            """, nativeQuery = true)
    List<String> searchIds(UUID userId, String needle, String project, int limit);

    @Query("SELECT n FROM InventoryNeedEntity n LEFT JOIN FETCH n.specifications "
            + "WHERE n.userId = :userId AND n.id IN :ids")
    List<InventoryNeedEntity> fetchSelected(UUID userId, List<String> ids);
}
