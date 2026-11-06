package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.config.LedgerProperties;
import com.yourlifeone.finance.ledger.domain.Account;
import com.yourlifeone.finance.ledger.domain.BalanceChange;
import com.yourlifeone.finance.ledger.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.AccountRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Applies a balance change to the user's accounts inside the caller's transaction. The accounts are
 * locked first, so parallel writes on one account apply one after another and none is lost.
 */
@Component
@RequiredArgsConstructor
public class BalanceUpdater {
    private static final Comparator<Account> LISTED_ORDER = Comparator.comparingInt(Account::sortOrder)
            .thenComparing(Account::createdAt).thenComparing(Account::id);

    private final AccountRepository accounts;
    private final LedgerPersistenceMapper persistence;
    private final LedgerProperties properties;

    /**
     * @param required accounts that must exist (those the saved transaction names)
     * @return the accounts whose balance changed, in the order the app lists them
     */
    public List<Account> apply(UUID userId, BalanceChange change, Collection<String> required, Instant now) {
        var ids = new HashSet<>(change.accountIds());
        ids.addAll(required);
        var locked = accounts.lockAll(userId, ids);
        requireExisting(locked, required);
        return locked.stream()
                .filter(account -> change.deltaFor(account.getId()) != 0)
                .map(account -> move(account, change.deltaFor(account.getId()), now))
                .map(persistence::toDomain)
                .sorted(LISTED_ORDER)
                .toList();
    }

    private AccountEntity move(AccountEntity account, long delta, Instant now) {
        var balance = Math.addExact(account.getBalance(), delta);
        if (Math.abs(balance) > properties.maxBalance()) {
            throw ApiException.badRequest("invalid_balance",
                    "This would take the account's balance beyond " + properties.maxBalance() + " pence.");
        }
        account.setBalance(balance);
        account.setUpdatedAt(now);
        return accounts.save(account);
    }

    private static void requireExisting(List<AccountEntity> locked, Collection<String> required) {
        var found = locked.stream().map(AccountEntity::getId).toList();
        if (!found.containsAll(required)) {
            throw ApiException.notFound("account_not_found", "This account does not exist.");
        }
    }
}
