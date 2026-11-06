package com.yourlifeone.life.trips.infrastructure.persistence.repository;

import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.ItineraryItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItemEntity, UserScopedId> {
    List<ItineraryItemEntity> findByUserIdAndTripIdOrderByDateAscSortOrderAsc(UUID userId, String tripId);
}
