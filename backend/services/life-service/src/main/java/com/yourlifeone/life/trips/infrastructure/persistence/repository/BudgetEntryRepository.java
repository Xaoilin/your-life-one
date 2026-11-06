package com.yourlifeone.life.trips.infrastructure.persistence.repository;

import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BudgetEntryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetEntryRepository extends JpaRepository<BudgetEntryEntity, UserScopedId> {
    List<BudgetEntryEntity> findByUserIdAndTripIdOrderByCreatedAt(UUID userId, String tripId);
}
