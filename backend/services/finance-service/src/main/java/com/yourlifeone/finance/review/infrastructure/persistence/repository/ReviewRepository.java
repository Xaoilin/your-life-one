package com.yourlifeone.finance.review.infrastructure.persistence.repository;

import com.yourlifeone.finance.review.infrastructure.persistence.entity.ReviewEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    /** The user's review, locked until the transaction ends, so two saves never both pass the version check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReviewEntity r WHERE r.userId = :userId")
    Optional<ReviewEntity> lockByUserId(UUID userId);
}
