package com.yourlifeone.planner.clock.infrastructure.persistence.repository;

import com.yourlifeone.planner.clock.infrastructure.persistence.entity.CounterEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CounterRepository extends JpaRepository<CounterEntity, UUID> {
    @Modifying
    @Query(value = "INSERT INTO clock.counter (user_id) VALUES (:userId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(UUID userId);

    /** The user's counters, locked until the transaction ends, so two new stopwatches never share a number. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CounterEntity c WHERE c.userId = :userId")
    Optional<CounterEntity> lockByUserId(UUID userId);
}
