package com.yourlifeone.planner.gamification.infrastructure.persistence.repository;

import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.PrayerRewardEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.PrayerRewardId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PrayerRewardRepository extends JpaRepository<PrayerRewardEntity, PrayerRewardId> {
    List<PrayerRewardEntity> findByUserId(UUID userId);

    /** After a progress reset every receipt is history: kept, so no prayer is rewarded again, never taken back. */
    @Modifying
    @Query("UPDATE PrayerRewardEntity r SET r.source = 'reset' WHERE r.userId = :userId AND r.source = 'planner'")
    int markHistory(UUID userId);
}
