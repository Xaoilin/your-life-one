package com.yourlifeone.knowledge.knowledge.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.knowledge.domain.Topic;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.mapper.KnowledgePersistenceMapper;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.TopicRepository;
import com.yourlifeone.knowledge.shared.RecordIds;
import com.yourlifeone.knowledge.shared.Texts;
import com.yourlifeone.knowledge.shared.Timestamps;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Knowledge-base topics. A save replaces the topic; a delete removes it with its entries. */
@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topics;
    private final KnowledgePersistenceMapper persistence;
    private final Clock clock;

    /** Creates the topic (placed last unless it names a position), or replaces it keeping its creation time. */
    @Transactional
    public Topic save(UUID userId, Topic requested) {
        var key = new UserScopedId(userId, RecordIds.require(requested.id()));
        var existing = topics.findById(key);
        var now = Timestamps.now(clock);
        var sortOrder = requested.sortOrder() != null ? requested.sortOrder()
                : existing.map(topic -> topic.getSortOrder()).orElseGet(() -> (int) topics.countByUserId(userId));
        var topic = requested.toBuilder()
                .name(requested.name().trim())
                .description(Texts.trimmed(requested.description()))
                .icon(Texts.trimmed(requested.icon()))
                .color(Texts.trimmed(requested.color()))
                .sortOrder(sortOrder)
                .createdAt(existing.map(entity -> entity.getCreatedAt()).orElse(now))
                .updatedAt(now)
                .build();
        return persistence.toDomain(topics.save(persistence.toEntity(userId, topic)));
    }

    /** Deletes the topic; the database deletes its entries with it. */
    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!topics.existsById(key)) {
            throw ApiException.notFound("topic_not_found", "This topic does not exist.");
        }
        topics.deleteById(key);
    }
}
