package com.yourlifeone.planner.gamification.infrastructure.persistence.repository;

import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.MomentumEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.MomentumId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentumRepository extends JpaRepository<MomentumEntity, MomentumId> {
    List<MomentumEntity> findByUserId(UUID userId);
}
