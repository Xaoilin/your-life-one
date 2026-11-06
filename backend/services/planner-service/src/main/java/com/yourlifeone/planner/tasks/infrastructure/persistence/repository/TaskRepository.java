package com.yourlifeone.planner.tasks.infrastructure.persistence.repository;

import com.yourlifeone.planner.shared.UserScopedId;
import com.yourlifeone.planner.tasks.infrastructure.persistence.entity.TaskEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, UserScopedId> {
    List<TaskEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    List<TaskEntity> findByUserIdAndProjectId(UUID userId, String projectId);
}
