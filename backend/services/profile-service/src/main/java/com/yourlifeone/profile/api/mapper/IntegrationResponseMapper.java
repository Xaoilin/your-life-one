package com.yourlifeone.profile.api.mapper;

import com.yourlifeone.profile.api.response.IntegrationResponse;
import com.yourlifeone.profile.domain.Integration;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface IntegrationResponseMapper {
    IntegrationResponse toResponse(Integration integration);

    List<IntegrationResponse> toResponses(List<Integration> integrations);
}
