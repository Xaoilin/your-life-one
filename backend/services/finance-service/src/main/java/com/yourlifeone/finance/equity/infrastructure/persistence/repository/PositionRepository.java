package com.yourlifeone.finance.equity.infrastructure.persistence.repository;

import com.yourlifeone.finance.equity.infrastructure.persistence.entity.PositionEntity;
import com.yourlifeone.finance.shared.UserScopedId;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PositionRepository extends JpaRepository<PositionEntity, UserScopedId> {
    /** Positions whose company contains the text, ignoring case (wildcards in it match themselves). */
    @Query(value = "SELECT * FROM equity.position WHERE user_id = :userId "
            + "AND strpos(upper(company), upper(:company)) > 0 ORDER BY id LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<PositionEntity> search(UUID userId, String company, int limit, int offset);

    @Query(value = "SELECT count(*) FROM equity.position WHERE user_id = :userId "
            + "AND strpos(upper(company), upper(:company)) > 0", nativeQuery = true)
    int countMatches(UUID userId, String company);

    long countByUserId(UUID userId);

    /** The position, locked until the transaction ends, so two edits never both pass the version check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PositionEntity p WHERE p.userId = :userId AND p.id = :id")
    Optional<PositionEntity> lock(UUID userId, String id);
}
