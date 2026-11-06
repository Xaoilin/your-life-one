package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.model.AccountSyncResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarSyncFreshnessTest {
    private final CalendarSyncFreshness freshness = new CalendarSyncFreshness();
    private final UUID user = UUID.randomUUID();
    private final AccountSyncResult success = AccountSyncResult.builder().accountId("account")
            .status("synced").eventCount(3).build();

    @Test
    void onlyRequestsStartedBeforeCompletionShareTheResult() {
        var requested = freshness.start();
        freshness.complete(user, freshness.start(), success);
        assertEquals(success, freshness.completedSince(user, "account", requested).orElseThrow());
        assertTrue(freshness.completedSince(user, "account", freshness.start()).isEmpty());
    }

    @Test
    void usersAndAccountsNeverShareResults() {
        var requested = freshness.start();
        freshness.complete(user, freshness.start(), success);
        assertTrue(freshness.completedSince(UUID.randomUUID(), "account", requested).isEmpty());
        assertTrue(freshness.completedSince(user, "other-account", requested).isEmpty());
    }

    @Test
    void failedProviderResultsAreNotShared() {
        var requested = freshness.start();
        freshness.complete(user, freshness.start(), AccountSyncResult.builder()
                .accountId("account").status("failed").build());
        assertTrue(freshness.completedSince(user, "account", requested).isEmpty());
    }

    @Test
    void mutationDuringSyncPreventsPublishingItsResult() {
        var started = freshness.start();
        freshness.invalidate(user);
        freshness.complete(user, started, success);
        assertTrue(freshness.completedSince(user, "account", started).isEmpty());
    }

    @Test
    void mutationCommitInvalidatesSyncThatFinishedBeforeTheCommit() {
        var requested = freshness.start();
        TransactionSynchronizationManager.initSynchronization();
        try {
            freshness.invalidate(user);
            freshness.complete(user, freshness.start(), success);
            assertTrue(freshness.completedSince(user, "account", requested).isPresent());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertTrue(freshness.completedSince(user, "account", requested).isEmpty());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
