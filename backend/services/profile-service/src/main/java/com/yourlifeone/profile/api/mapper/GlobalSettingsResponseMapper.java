package com.yourlifeone.profile.api.mapper;

import com.yourlifeone.profile.api.response.GlobalSettingsResponse;
import com.yourlifeone.profile.domain.GlobalSettings;
import org.mapstruct.Mapper;

@Mapper
public interface GlobalSettingsResponseMapper {
    GlobalSettingsResponse toResponse(GlobalSettings settings);
}
