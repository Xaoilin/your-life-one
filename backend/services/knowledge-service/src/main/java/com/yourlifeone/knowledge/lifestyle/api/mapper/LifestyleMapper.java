package com.yourlifeone.knowledge.lifestyle.api.mapper;

import com.yourlifeone.knowledge.lifestyle.api.request.LifestyleItemRequest;
import com.yourlifeone.knowledge.lifestyle.api.response.LifestyleItemResponse;
import com.yourlifeone.knowledge.lifestyle.api.response.LifestyleItemsResponse;
import com.yourlifeone.knowledge.lifestyle.domain.LifestyleItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LifestyleMapper {
    LifestyleItemResponse toResponse(LifestyleItem item);

    List<LifestyleItemResponse> toResponses(List<LifestyleItem> items);

    default LifestyleItemsResponse toItemsResponse(List<LifestyleItem> items) {
        return new LifestyleItemsResponse(toResponses(items));
    }

    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LifestyleItem toDomain(String id, LifestyleItemRequest request);
}
