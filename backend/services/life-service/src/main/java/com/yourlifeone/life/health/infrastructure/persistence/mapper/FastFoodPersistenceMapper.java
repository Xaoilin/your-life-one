package com.yourlifeone.life.health.infrastructure.persistence.mapper;

import com.yourlifeone.life.health.domain.FastFoodEntry;
import com.yourlifeone.life.health.infrastructure.persistence.entity.FastFoodEntryEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface FastFoodPersistenceMapper {
    FastFoodEntry toDomain(FastFoodEntryEntity entity);

    FastFoodEntryEntity toEntity(UUID userId, FastFoodEntry entry);
}
