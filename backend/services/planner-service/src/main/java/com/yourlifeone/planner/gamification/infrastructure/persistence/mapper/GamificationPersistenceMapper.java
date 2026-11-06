package com.yourlifeone.planner.gamification.infrastructure.persistence.mapper;

import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.ProfileEntity;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface GamificationPersistenceMapper {
    @Mapping(target = "habitTallies", source = "habitTallies")
    Profile toDomain(ProfileEntity entity, Map<String, Integer> habitTallies);

    @Mapping(target = "updatedAt", ignore = true)
    ProfileEntity toEntity(UUID userId, Profile profile);
}
