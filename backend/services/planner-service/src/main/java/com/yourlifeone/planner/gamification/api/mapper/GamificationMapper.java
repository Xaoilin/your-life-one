package com.yourlifeone.planner.gamification.api.mapper;

import com.yourlifeone.planner.gamification.api.response.GamificationResponse;
import com.yourlifeone.planner.gamification.api.response.RewardResponse;
import com.yourlifeone.planner.gamification.domain.Levels;
import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.gamification.domain.Reward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(imports = Levels.class)
public interface GamificationMapper {
    @Mapping(target = "title", expression = "java(Levels.titleForLevel(profile.level()))")
    GamificationResponse toResponse(Profile profile);

    RewardResponse toResponse(Reward reward);
}
