package com.yourlifeone.finance.ledger.infrastructure.persistence.repository;

import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.SavingsGoalEntity;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, UserScopedId> {
    List<SavingsGoalEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    /** Goals saved in a deleted account keep going, linked to no account. */
    @Modifying
    @Query("UPDATE SavingsGoalEntity g SET g.linkedAccountId = NULL, g.updatedAt = :now"
            + " WHERE g.userId = :userId AND g.linkedAccountId = :accountId")
    int unlinkAccount(UUID userId, String accountId, Instant now);
}
