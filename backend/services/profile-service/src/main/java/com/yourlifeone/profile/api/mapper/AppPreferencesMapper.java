package com.yourlifeone.profile.api.mapper;

import com.yourlifeone.profile.api.request.AppPreferencesRequest;
import com.yourlifeone.profile.api.response.AppPreferencesResponse;
import com.yourlifeone.profile.domain.AppPreferences;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AppPreferencesMapper {
    AppPreferencesResponse toResponse(AppPreferences preferences);

    @Mapping(target = "updatedAt", ignore = true)
    AppPreferences toDomain(AppPreferencesRequest request);
}
