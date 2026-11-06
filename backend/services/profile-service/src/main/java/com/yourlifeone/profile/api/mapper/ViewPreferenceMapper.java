package com.yourlifeone.profile.api.mapper;

import com.yourlifeone.profile.api.response.ViewPreferenceResponse;
import com.yourlifeone.profile.domain.ViewPreference;
import org.mapstruct.Mapper;

@Mapper
public interface ViewPreferenceMapper {
    ViewPreferenceResponse toResponse(ViewPreference preference);
}
