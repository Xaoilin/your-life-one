package com.yourlifeone.life.health.infrastructure.persistence.repository;

import com.yourlifeone.life.health.infrastructure.persistence.entity.FastFoodEntryEntity;
import com.yourlifeone.life.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FastFoodEntryRepository extends JpaRepository<FastFoodEntryEntity, UserScopedId> {
    List<FastFoodEntryEntity> findByUserIdOrderByDateDescCreatedAtDesc(UUID userId);
}
