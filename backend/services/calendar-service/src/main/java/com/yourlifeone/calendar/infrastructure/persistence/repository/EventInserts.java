package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import java.util.Collection;

/** Inserts events known to be new without first looking each one up, so a first sync is one batch. */
public interface EventInserts {
    void insertAll(Collection<EventEntity> newEvents);
}
