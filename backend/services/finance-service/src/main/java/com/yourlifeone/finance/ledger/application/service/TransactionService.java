package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.domain.BalanceChange;
import com.yourlifeone.finance.ledger.domain.Transaction;
import com.yourlifeone.finance.ledger.domain.TransactionDeleted;
import com.yourlifeone.finance.ledger.domain.TransactionSaved;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.TransactionRepository;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Timestamps;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactions, with their effect on account balances owned here: a new transaction applies its effect, a
 * replacement reverts the old effect and applies the new one, and a delete reverts it.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactions;
    private final BalanceUpdater balances;
    private final LedgerPersistenceMapper persistence;
    private final LedgerRules rules;
    private final Clock clock;

    @Transactional
    public TransactionSaved save(UUID userId, Transaction requested) {
        var transaction = rules.transaction(requested);
        var now = Timestamps.now(clock);
        var previous = transactions.findById(new UserScopedId(userId, transaction.id())).map(persistence::toDomain);
        var change = new BalanceChange();
        previous.ifPresent(change::revert);
        change.apply(transaction);
        var changed = balances.apply(userId, change, accountsOf(transaction), now);
        var stored = transaction.toBuilder()
                .createdAt(previous.map(Transaction::createdAt).orElse(now))
                .updatedAt(now)
                .build();
        transactions.save(persistence.toEntity(userId, stored));
        return new TransactionSaved(stored, changed);
    }

    @Transactional
    public TransactionDeleted delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        var transaction = transactions.findById(key).map(persistence::toDomain)
                .orElseThrow(() -> ApiException.notFound("transaction_not_found", "This transaction does not exist."));
        var changed = balances.apply(userId, new BalanceChange().revert(transaction), List.of(), Timestamps.now(clock));
        transactions.deleteById(key);
        return new TransactionDeleted(id, changed);
    }

    private static List<String> accountsOf(Transaction transaction) {
        return Stream.of(transaction.accountId(), transaction.toAccountId()).filter(account -> account != null).toList();
    }
}
