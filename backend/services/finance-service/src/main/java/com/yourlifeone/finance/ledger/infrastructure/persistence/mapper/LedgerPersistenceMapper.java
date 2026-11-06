package com.yourlifeone.finance.ledger.infrastructure.persistence.mapper;

import com.yourlifeone.finance.ledger.domain.Account;
import com.yourlifeone.finance.ledger.domain.Budget;
import com.yourlifeone.finance.ledger.domain.SavingsGoal;
import com.yourlifeone.finance.ledger.domain.Transaction;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.BudgetEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.SavingsGoalEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.TransactionEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface LedgerPersistenceMapper {
    Account toDomain(AccountEntity entity);

    Transaction toDomain(TransactionEntity entity);

    Budget toDomain(BudgetEntity entity);

    SavingsGoal toDomain(SavingsGoalEntity entity);

    AccountEntity toEntity(UUID userId, Account account);

    TransactionEntity toEntity(UUID userId, Transaction transaction);

    BudgetEntity toEntity(UUID userId, Budget budget);

    SavingsGoalEntity toEntity(UUID userId, SavingsGoal goal);
}
