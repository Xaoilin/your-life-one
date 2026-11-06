package com.yourlifeone.knowledge.knowledge.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.knowledge.domain.Entry;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.mapper.KnowledgePersistenceMapper;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.EntryRepository;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.TopicRepository;
import com.yourlifeone.knowledge.shared.RecordIds;
import com.yourlifeone.knowledge.shared.Timestamps;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-base entries. A save replaces the entry (moving it to another topic too); a delete removes one. */
@Service
@RequiredArgsConstructor
public class EntryService {
    private final EntryRepository entries;
    private final TopicRepository topics;
    private final KnowledgeRules rules;
    private final KnowledgePersistenceMapper persistence;
    private final Clock clock;

    @Transactional
    public Entry save(UUID userId, Entry requested) {
        var key = new UserScopedId(userId, RecordIds.require(requested.id()));
        if (!topics.existsById(new UserScopedId(userId, RecordIds.require(requested.topicId())))) {
            throw ApiException.notFound("topic_not_found", "The entry's topic does not exist.");
        }
        var now = Timestamps.now(clock);
        var entry = requested.toBuilder()
                .title(requested.title().trim())
                .content(requested.content() == null ? "" : requested.content())
                .sources(rules.sources(requested.sources()))
                .tags(rules.tags(requested.tags()))
                .createdAt(entries.findById(key).map(existing -> existing.getCreatedAt()).orElse(now))
                .updatedAt(now)
                .build();
        return persistence.toDomain(entries.save(persistence.toEntity(userId, entry)));
    }

    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!entries.existsById(key)) {
            throw ApiException.notFound("entry_not_found", "This entry does not exist.");
        }
        entries.deleteById(key);
    }
}
