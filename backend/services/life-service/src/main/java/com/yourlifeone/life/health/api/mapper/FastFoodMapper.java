package com.yourlifeone.life.health.api.mapper;

import com.yourlifeone.life.health.api.request.FastFoodEntryRequest;
import com.yourlifeone.life.health.api.response.FastFoodEntryResponse;
import com.yourlifeone.life.health.domain.FastFoodEntry;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FastFoodMapper {
    FastFoodEntryResponse toResponse(FastFoodEntry entry);

    List<FastFoodEntryResponse> toResponses(List<FastFoodEntry> entries);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FastFoodEntry toDomain(String id, FastFoodEntryRequest request);
}
