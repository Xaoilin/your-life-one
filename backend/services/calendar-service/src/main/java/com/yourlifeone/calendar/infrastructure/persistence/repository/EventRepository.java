package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.UserScopedId;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<EventEntity, UserScopedId>, EventInserts, EventComparisons {
    /** Events overlapping {@code [from, to)}; zero-length events at {@code from} are included. */
    List<EventEntity> findByUserIdAndStartsAtLessThanAndEndsAtGreaterThanEqualOrderByStartsAtAscIdAsc(
            UUID userId, Instant to, Instant from);

    List<EventEntity> findByUserIdAndSourceIdAndIdIn(UUID userId, String sourceId, Collection<String> ids);

    @Modifying(flushAutomatically = true)
    @Query("delete from EventEntity e where e.userId = :userId and e.sourceId = :sourceId and e.id in :ids")
    void deleteMirrored(UUID userId, String sourceId, Collection<String> ids);

    Optional<EventEntity> findByUserIdAndId(UUID userId, String id);
}
