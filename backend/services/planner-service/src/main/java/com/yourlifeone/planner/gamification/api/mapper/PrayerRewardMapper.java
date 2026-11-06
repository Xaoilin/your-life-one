package com.yourlifeone.planner.gamification.api.mapper;

import com.yourlifeone.planner.gamification.api.response.PrayerRewardSyncResponse;
import com.yourlifeone.planner.gamification.domain.PrayerRewardSync;
import com.yourlifeone.planner.tasks.api.mapper.TasksMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {GamificationMapper.class, TasksMapper.class})
public interface PrayerRewardMapper {
    PrayerRewardSyncResponse toResponse(PrayerRewardSync sync);
}
