package com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.mapper;

import com.yourlifeone.knowledge.lifestyle.domain.LifestyleItem;
import com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.entity.LifestyleItemEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface LifestylePersistenceMapper {
    LifestyleItem toDomain(LifestyleItemEntity entity);

    LifestyleItemEntity toEntity(UUID userId, LifestyleItem item);
}
