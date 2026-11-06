package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.finance.ledger.domain.Ledger;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.AccountRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.BudgetRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.SavingsGoalRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.TransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the whole ledger in the order the app lists it. */
@Service
@RequiredArgsConstructor
public class LedgerService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final SavingsGoalRepository goals;
    private final LedgerPersistenceMapper persistence;

    @Transactional(readOnly = true)
    public Ledger ledger(UUID userId) {
        return new Ledger(
                accounts.findByUserIdOrderBySortOrderAscCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain).toList(),
                transactions.findByUserIdOrderByDateDescCreatedAtDescIdDesc(userId).stream().map(persistence::toDomain).toList(),
                budgets.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain).toList(),
                goals.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain).toList());
    }
}
