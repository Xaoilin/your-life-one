package com.yourlifeone.planner.clock.infrastructure.persistence.repository;

import com.yourlifeone.planner.clock.infrastructure.persistence.entity.StopwatchEntity;
import com.yourlifeone.planner.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopwatchRepository extends JpaRepository<StopwatchEntity, UserScopedId> {
    List<StopwatchEntity> findByUserIdOrderByPositionAscCreatedAtAsc(UUID userId);
}
