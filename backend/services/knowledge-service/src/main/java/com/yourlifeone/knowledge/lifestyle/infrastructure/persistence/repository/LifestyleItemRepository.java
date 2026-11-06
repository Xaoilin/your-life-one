package com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.repository;

import com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.entity.LifestyleItemEntity;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifestyleItemRepository extends JpaRepository<LifestyleItemEntity, UserScopedId> {
    List<LifestyleItemEntity> findByUserIdOrderBySortOrderAscCreatedAtAscIdAsc(UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndTypeAndStatus(UUID userId, String type, String status);
}
