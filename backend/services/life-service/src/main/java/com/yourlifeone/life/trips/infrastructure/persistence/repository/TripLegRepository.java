package com.yourlifeone.life.trips.infrastructure.persistence.repository;

import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripLegEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripLegRepository extends JpaRepository<TripLegEntity, UserScopedId> {
    List<TripLegEntity> findByUserIdAndTripIdOrderBySortOrder(UUID userId, String tripId);
}
