package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.domain.SavingsGoal;
import com.yourlifeone.finance.ledger.infrastructure.persistence.mapper.LedgerPersistenceMapper;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.AccountRepository;
import com.yourlifeone.finance.ledger.infrastructure.persistence.repository.SavingsGoalRepository;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Timestamps;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Savings goals. The completion time is stamped here: when a goal becomes completed, kept while it stays
 * so, and cleared when it is reopened.
 */
@Service
@RequiredArgsConstructor
public class SavingsGoalService {
    private final SavingsGoalRepository goals;
    private final AccountRepository accounts;
    private final LedgerPersistenceMapper persistence;
    private final LedgerRules rules;
    private final Clock clock;

    @Transactional
    public SavingsGoal save(UUID userId, SavingsGoal requested) {
        var goal = rules.savingsGoal(requested);
        requireLinkedAccount(userId, goal.linkedAccountId());
        var now = Timestamps.now(clock);
        var previous = goals.findById(new UserScopedId(userId, goal.id())).map(persistence::toDomain);
        var stored = goal.toBuilder()
                .completedAt(completedAt(goal.completed(), previous, now))
                .createdAt(previous.map(SavingsGoal::createdAt).orElse(now))
                .updatedAt(now)
                .build();
        return persistence.toDomain(goals.save(persistence.toEntity(userId, stored)));
    }

    @Transactional
    public String delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!goals.existsById(key)) {
            throw ApiException.notFound("savings_goal_not_found", "This savings goal does not exist.");
        }
        goals.deleteById(key);
        return id;
    }

    private static Instant completedAt(boolean completed, Optional<SavingsGoal> previous, Instant now) {
        if (!completed) return null;
        return previous.filter(SavingsGoal::completed).map(SavingsGoal::completedAt).orElse(now);
    }

    private void requireLinkedAccount(UUID userId, String accountId) {
        if (accountId != null && !accounts.existsById(new UserScopedId(userId, accountId))) {
            throw ApiException.notFound("account_not_found", "This account does not exist.");
        }
    }
}
