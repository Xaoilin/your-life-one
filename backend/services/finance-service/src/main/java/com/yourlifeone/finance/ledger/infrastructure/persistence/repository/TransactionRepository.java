package com.yourlifeone.finance.ledger.infrastructure.persistence.repository;

import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.yourlifeone.finance.shared.UserScopedId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UserScopedId> {
    List<TransactionEntity> findByUserIdOrderByDateDescCreatedAtDescIdDesc(UUID userId);

    /** Transactions on either side of the account. */
    @Query("SELECT t.id FROM TransactionEntity t WHERE t.userId = :userId"
            + " AND (t.accountId = :accountId OR t.toAccountId = :accountId) ORDER BY t.id")
    List<String> findIdsTouchingAccount(UUID userId, String accountId);

    @Modifying
    @Query("DELETE FROM TransactionEntity t WHERE t.userId = :userId AND t.id IN :ids")
    int deleteAllByIds(UUID userId, Collection<String> ids);
}
