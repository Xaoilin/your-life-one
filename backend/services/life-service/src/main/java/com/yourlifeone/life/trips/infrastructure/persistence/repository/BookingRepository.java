package com.yourlifeone.life.trips.infrastructure.persistence.repository;

import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BookingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, UserScopedId> {
    List<BookingEntity> findByUserIdAndTripIdOrderByCreatedAt(UUID userId, String tripId);
}
