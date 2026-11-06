package com.yourlifeone.planner.gamification.infrastructure.persistence.repository;

import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.HabitTallyEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.HabitTallyId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitTallyRepository extends JpaRepository<HabitTallyEntity, HabitTallyId> {
    List<HabitTallyEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
