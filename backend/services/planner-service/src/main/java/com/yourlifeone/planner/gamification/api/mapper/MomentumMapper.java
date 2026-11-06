package com.yourlifeone.planner.gamification.api.mapper;

import com.yourlifeone.planner.gamification.api.response.MomentumPillarResponse;
import com.yourlifeone.planner.gamification.api.response.MomentumResponse;
import com.yourlifeone.planner.gamification.domain.MomentumPillar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface MomentumMapper {
    MomentumPillarResponse toResponse(MomentumPillar pillar);

    List<MomentumPillarResponse> toResponses(List<MomentumPillar> pillars);

    default MomentumResponse toMomentumResponse(Collection<MomentumPillar> pillars) {
        return new MomentumResponse(toResponses(pillars.stream()
                .sorted(Comparator.comparing(MomentumPillar::pillar)).toList()));
    }
}
