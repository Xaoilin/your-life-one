package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.domain.Account;
import com.yourlifeone.finance.ledger.domain.AccountDeleted;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.AccountRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.SavingsGoalRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.TransactionRepository;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Timestamps;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts. Saving one sets every field as given, its balance included; deleting one removes every
 * transaction on either side of it and unlinks the savings goals kept in it.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final SavingsGoalRepository goals;
    private final LedgerPersistenceMapper persistence;
    private final LedgerRules rules;
    private final Clock clock;

    @Transactional
    public Account save(UUID userId, Account requested) {
        var account = rules.account(requested);
        var now = Timestamps.now(clock);
        var createdAt = accounts.findById(new UserScopedId(userId, account.id()))
                .map(AccountEntity::getCreatedAt)
                .orElse(now);
        var saved = accounts.save(persistence.toEntity(userId, account.toBuilder().createdAt(createdAt).updatedAt(now).build()));
        return persistence.toDomain(saved);
    }

    @Transactional
    public AccountDeleted delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!accounts.existsById(key)) {
            throw ApiException.notFound("account_not_found", "This account does not exist.");
        }
        var removed = transactions.findIdsTouchingAccount(userId, id);
        if (!removed.isEmpty()) transactions.deleteAllByIds(userId, removed);
        goals.unlinkAccount(userId, id, Timestamps.now(clock));
        accounts.deleteById(key);
        return new AccountDeleted(id, removed);
    }
}
