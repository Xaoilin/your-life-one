package com.yourlifeone.knowledge.knowledge.application.service;

import com.yourlifeone.knowledge.knowledge.domain.KnowledgeBase;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.mapper.KnowledgePersistenceMapper;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.EntryRepository;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.TopicRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the whole knowledge base. */
@Service
@RequiredArgsConstructor
public class KnowledgeQueries {
    private final TopicRepository topics;
    private final EntryRepository entries;
    private final KnowledgePersistenceMapper persistence;

    @Transactional(readOnly = true)
    public KnowledgeBase knowledgeBase(UUID userId) {
        return new KnowledgeBase(
                topics.findByUserIdOrderBySortOrderAscNameAsc(userId).stream().map(persistence::toDomain).toList(),
                entries.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain).toList());
    }
}
