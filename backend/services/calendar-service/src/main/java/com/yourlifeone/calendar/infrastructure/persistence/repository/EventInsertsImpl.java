package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

/**
 * Spring Data's {@code save} merges entities with assigned IDs, which reads each row first; these
 * events are new, so they are persisted directly and batched by Hibernate.
 */
@RequiredArgsConstructor
class EventInsertsImpl implements EventInserts {
    private final EntityManager entityManager;

    @Override
    public void insertAll(Collection<EventEntity> newEvents) {
        newEvents.forEach(entityManager::persist);
    }
}
