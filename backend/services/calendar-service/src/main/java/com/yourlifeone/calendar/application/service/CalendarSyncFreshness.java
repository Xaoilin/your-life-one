package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.model.AccountSyncResult;
import com.yourlifeone.calendar.application.model.CompletedAccountSync;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Shares a completed provider pass only with requests already waiting for it. A later explicit
 * refresh always reaches Google; there is no time-based suppression or dependency on wall clocks.
 * State has the same process scope as CalendarSyncService's existing user lock.
 */
@Component
public class CalendarSyncFreshness {
    private long sequence;
    private final Map<UUID, Long> invalidations = new HashMap<>();
    private final Map<UUID, Map<String, CompletedAccountSync>> completed = new HashMap<>();

    public synchronized long start() {
        return ++sequence;
    }

    public synchronized Optional<AccountSyncResult> completedSince(UUID userId, String accountId, long request) {
        var accounts = completed.get(userId);
        var latest = accounts == null ? null : accounts.get(accountId);
        return latest != null && latest.sequence() > request ? Optional.of(latest.result()) : Optional.empty();
    }

    public synchronized void complete(UUID userId, long started, AccountSyncResult result) {
        if (!"synced".equals(result.status()) || invalidations.getOrDefault(userId, 0L) > started) return;
        completed.computeIfAbsent(userId, ignored -> new HashMap<>())
                .put(result.accountId(), new CompletedAccountSync(++sequence, result));
    }

    /**
     * Conservatively invalidates all this user's accounts, including source moves and deletions.
     * Clear both now and after commit so an in-flight sync cannot publish a pre-mutation result.
     */
    public void invalidate(UUID userId) {
        clear(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clear(userId);
                }
            });
        }
    }

    private synchronized void clear(UUID userId) {
        completed.remove(userId);
        invalidations.put(userId, ++sequence);
    }
}
