package com.yourlifeone.planner.gamification.infrastructure.persistence.repository;

import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.DailyLogEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.DailyLogId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyLogRepository extends JpaRepository<DailyLogEntity, DailyLogId> {
    void deleteByUserIdAndDayAndTaskIdIn(UUID userId, LocalDate day, Collection<String> taskIds);

    void deleteByUserId(UUID userId);
}
