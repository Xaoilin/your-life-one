package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.domain.Budget;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.BudgetEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.BudgetRepository;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Timestamps;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monthly budgets, one per expense category (the unique constraint keeps it so under races too). */
@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgets;
    private final LedgerPersistenceMapper persistence;
    private final LedgerRules rules;
    private final Clock clock;

    @Transactional
    public Budget save(UUID userId, Budget requested) {
        var budget = rules.budget(requested);
        if (budgets.existsByUserIdAndCategoryAndIdNot(userId, budget.category(), budget.id())) {
            throw ApiException.conflict("budget_exists", "This category already has a budget.");
        }
        var now = Timestamps.now(clock);
        var createdAt = budgets.findById(new UserScopedId(userId, budget.id()))
                .map(BudgetEntity::getCreatedAt)
                .orElse(now);
        var saved = budgets.save(persistence.toEntity(userId, budget.toBuilder().createdAt(createdAt).updatedAt(now).build()));
        return persistence.toDomain(saved);
    }

    @Transactional
    public String delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!budgets.existsById(key)) throw ApiException.notFound("budget_not_found", "This budget does not exist.");
        budgets.deleteById(key);
        return id;
    }
}
