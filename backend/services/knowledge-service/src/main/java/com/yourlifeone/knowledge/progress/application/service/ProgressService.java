package com.yourlifeone.knowledge.progress.application.service;

import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.EntryRepository;
import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository.TopicRepository;
import com.yourlifeone.knowledge.lifestyle.infrastructure.persistence.repository.LifestyleItemRepository;
import com.yourlifeone.knowledge.progress.config.ProgressProperties;
import com.yourlifeone.knowledge.progress.domain.KnowledgeProgress;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Counts the user's knowledge entries, topics and lifestyle items for the planner's badges. */
@Service
@RequiredArgsConstructor
public class ProgressService {
    private final EntryRepository entries;
    private final TopicRepository topics;
    private final LifestyleItemRepository lifestyle;
    private final ProgressProperties properties;

    @Transactional(readOnly = true)
    public KnowledgeProgress progress(UUID userId) {
        return KnowledgeProgress.builder()
                .entries(entries.countByUserId(userId))
                .topics(topics.countByUserId(userId))
                .haramMastered(lifestyle.countByUserIdAndTypeAndStatus(userId, properties.haramType(),
                        properties.masteredStatus()))
                .halalConsistent(lifestyle.countByUserIdAndTypeAndStatus(userId, properties.halalType(),
                        properties.consistentStatus()))
                .lifestyleItems(lifestyle.countByUserId(userId))
                .build();
    }
}
