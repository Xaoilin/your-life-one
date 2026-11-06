package com.yourlifeone.planner.infrastructure.upstream;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.infrastructure.upstream.dto.KnowledgeProgressDto;
import com.yourlifeone.planner.infrastructure.upstream.dto.RewardedOutcomeDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps the other services' answers to the planner's domain. */
@Mapper
public interface UpstreamMapper {
    PrayerOutcome toDomain(RewardedOutcomeDto outcome);

    List<PrayerOutcome> toDomain(List<RewardedOutcomeDto> outcomes);

    @Mapping(target = "knowledgeEntries", source = "entries")
    @Mapping(target = "knowledgeTopics", source = "topics")
    @Mapping(target = "lifestyleHaramMastered", source = "haramMastered")
    @Mapping(target = "lifestyleHalalConsistent", source = "halalConsistent")
    @Mapping(target = "lifestyleTotal", source = "lifestyleItems")
    KnowledgeProgress toDomain(KnowledgeProgressDto progress);
}
