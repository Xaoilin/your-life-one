package com.yourlifeone.planner.clock.infrastructure.persistence.repository;

import com.yourlifeone.planner.clock.infrastructure.persistence.entity.TimerEntity;
import com.yourlifeone.planner.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimerRepository extends JpaRepository<TimerEntity, UserScopedId> {
    List<TimerEntity> findByUserIdOrderByPositionAscCreatedAtAsc(UUID userId);
}
