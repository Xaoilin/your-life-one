package com.yourlifeone.knowledge.projects.infrastructure.persistence.repository;

import com.yourlifeone.knowledge.projects.infrastructure.persistence.entity.ProjectEntity;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UserScopedId> {
    List<ProjectEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndCatalogKey(UUID userId, String catalogKey);
}
