package com.yourlifeone.knowledge.projects.infrastructure.persistence.repository;

import com.yourlifeone.knowledge.projects.infrastructure.persistence.entity.PageEntity;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<PageEntity, UserScopedId> {
    List<PageEntity> findByUserIdOrderByOverviewDescUpdatedAtDescIdAsc(UUID userId);

    List<PageEntity> findByUserIdAndProjectIdAndOverviewTrue(UUID userId, String projectId);
}
