package com.yourlifeone.knowledge.knowledge.api.mapper;

import com.yourlifeone.knowledge.knowledge.api.request.EntryRequest;
import com.yourlifeone.knowledge.knowledge.api.request.TopicRequest;
import com.yourlifeone.knowledge.knowledge.api.response.EntryResponse;
import com.yourlifeone.knowledge.knowledge.api.response.KnowledgeResponse;
import com.yourlifeone.knowledge.knowledge.api.response.TopicResponse;
import com.yourlifeone.knowledge.knowledge.domain.Entry;
import com.yourlifeone.knowledge.knowledge.domain.KnowledgeBase;
import com.yourlifeone.knowledge.knowledge.domain.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface KnowledgeMapper {
    KnowledgeResponse toResponse(KnowledgeBase knowledgeBase);

    TopicResponse toResponse(Topic topic);

    EntryResponse toResponse(Entry entry);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Topic toDomain(String id, TopicRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Entry toDomain(String id, EntryRequest request);
}
