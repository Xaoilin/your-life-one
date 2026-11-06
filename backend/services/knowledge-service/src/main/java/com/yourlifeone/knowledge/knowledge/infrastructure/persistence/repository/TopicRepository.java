package com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository;

import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.entity.TopicEntity;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<TopicEntity, UserScopedId> {
    List<TopicEntity> findByUserIdOrderBySortOrderAscNameAsc(UUID userId);

    long countByUserId(UUID userId);
}
