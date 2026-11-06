package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.model.EventComparisonValue;
import com.yourlifeone.calendar.infrastructure.persistence.model.EventDifference;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventComparisons {
    List<EventDifference> differences(UUID userId, String sourceId, Collection<EventComparisonValue> fetched,
            Instant from, Instant to);
}
