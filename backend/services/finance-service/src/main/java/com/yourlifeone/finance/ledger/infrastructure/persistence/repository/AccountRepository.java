package com.yourlifeone.finance.ledger.infrastructure.persistence.repository;

import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.finance.shared.UserScopedId;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<AccountEntity, UserScopedId> {
    List<AccountEntity> findByUserIdOrderBySortOrderAscCreatedAtAscIdAsc(UUID userId);

    /**
     * The named accounts, locked until the transaction ends and always in ID order, so concurrent balance
     * changes apply one after another and never deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.userId = :userId AND a.id IN :ids ORDER BY a.id")
    List<AccountEntity> lockAll(UUID userId, Collection<String> ids);
}
