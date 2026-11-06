package com.yourlifeone.life.jobs.infrastructure.persistence.repository;

import com.yourlifeone.life.jobs.infrastructure.persistence.entity.JobApplicationEntity;
import com.yourlifeone.life.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobApplicationRepository extends JpaRepository<JobApplicationEntity, UserScopedId> {
    List<JobApplicationEntity> findByUserIdOrderByPosition(UUID userId);

    @Query(value = "SELECT a.id " + ApplicationSearchSql.MATCHING
            + " ORDER BY a.position LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<String> searchIds(UUID userId, String needle, String status, String workType, String remoteStatus,
            int limit, int offset);

    @Query(value = "SELECT count(*) " + ApplicationSearchSql.MATCHING, nativeQuery = true)
    int countMatches(UUID userId, String needle, String status, String workType, String remoteStatus);

    @Query("SELECT a FROM JobApplicationEntity a LEFT JOIN FETCH a.history "
            + "WHERE a.userId = :userId AND a.id IN :ids ORDER BY a.position")
    List<JobApplicationEntity> fetchSelected(UUID userId, List<String> ids);
}
