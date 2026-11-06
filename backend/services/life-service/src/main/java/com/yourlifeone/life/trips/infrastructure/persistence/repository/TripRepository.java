package com.yourlifeone.life.trips.infrastructure.persistence.repository;

import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<TripEntity, UserScopedId> {
    List<TripEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
