package com.yourlifeone.finance.ledger.infrastructure.persistence.repository;

import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.BudgetEntity;
import com.yourlifeone.finance.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UserScopedId> {
    List<BudgetEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    /** Whether another budget already has this category. */
    boolean existsByUserIdAndCategoryAndIdNot(UUID userId, String category, String id);
}
