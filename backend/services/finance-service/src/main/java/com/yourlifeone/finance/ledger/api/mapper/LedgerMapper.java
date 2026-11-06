package com.yourlifeone.finance.ledger.api.mapper;

import com.yourlifeone.finance.ledger.api.request.AccountRequest;
import com.yourlifeone.finance.ledger.api.request.BudgetRequest;
import com.yourlifeone.finance.ledger.api.request.SavingsGoalRequest;
import com.yourlifeone.finance.ledger.api.request.TransactionRequest;
import com.yourlifeone.finance.ledger.api.response.AccountDeletedResponse;
import com.yourlifeone.finance.ledger.api.response.AccountResponse;
import com.yourlifeone.finance.ledger.api.response.BudgetResponse;
import com.yourlifeone.finance.ledger.api.response.LedgerResponse;
import com.yourlifeone.finance.ledger.api.response.SavingsGoalResponse;
import com.yourlifeone.finance.ledger.api.response.TransactionDeletedResponse;
import com.yourlifeone.finance.ledger.api.response.TransactionSavedResponse;
import com.yourlifeone.finance.ledger.domain.Account;
import com.yourlifeone.finance.ledger.domain.AccountDeleted;
import com.yourlifeone.finance.ledger.domain.Budget;
import com.yourlifeone.finance.ledger.domain.Ledger;
import com.yourlifeone.finance.ledger.domain.SavingsGoal;
import com.yourlifeone.finance.ledger.domain.Transaction;
import com.yourlifeone.finance.ledger.domain.TransactionDeleted;
import com.yourlifeone.finance.ledger.domain.TransactionSaved;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LedgerMapper {
    LedgerResponse toResponse(Ledger ledger);

    AccountResponse toResponse(Account account);

    BudgetResponse toResponse(Budget budget);

    SavingsGoalResponse toResponse(SavingsGoal goal);

    TransactionSavedResponse toResponse(TransactionSaved saved);

    TransactionDeletedResponse toResponse(TransactionDeleted deleted);

    AccountDeletedResponse toResponse(AccountDeleted deleted);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toDomain(String id, AccountRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toDomain(String id, TransactionRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Budget toDomain(String id, BudgetRequest request);

    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SavingsGoal toDomain(String id, SavingsGoalRequest request);
}
