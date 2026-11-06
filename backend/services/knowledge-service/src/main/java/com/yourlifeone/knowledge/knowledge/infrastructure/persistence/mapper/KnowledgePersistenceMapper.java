package com.yourlifeone.knowledge.knowledge.infrastructure.persistence.mapper;

import com.yourlifeone.knowledge.knowledge.domain.Entry;
import com.yourlifeone.knowledge.knowledge.domain.Topic;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.entity.EntryEntity;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.entity.TopicEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface KnowledgePersistenceMapper {
    Topic toDomain(TopicEntity entity);

    TopicEntity toEntity(UUID userId, Topic topic);

    Entry toDomain(EntryEntity entity);

    EntryEntity toEntity(UUID userId, Entry entry);
}
