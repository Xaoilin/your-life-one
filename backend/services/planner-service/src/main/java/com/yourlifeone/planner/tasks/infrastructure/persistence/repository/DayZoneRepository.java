package com.yourlifeone.planner.tasks.infrastructure.persistence.repository;

import com.yourlifeone.planner.tasks.infrastructure.persistence.entity.DayZoneEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayZoneRepository extends JpaRepository<DayZoneEntity, UUID> {
}
