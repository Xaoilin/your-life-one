package com.yourlifeone.finance.review.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.review.api.request.LoanProfileChangeRequest;
import com.yourlifeone.finance.review.domain.FinanceReview;
import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import com.yourlifeone.finance.review.domain.LoanOverpaymentProfile;
import com.yourlifeone.finance.review.domain.ReviewBudgetForecast;
import com.yourlifeone.finance.review.infrastructure.persistence.entity.ReviewEntity;
import com.yourlifeone.finance.review.infrastructure.persistence.mapper.ReviewPersistenceMapper;
import com.yourlifeone.finance.review.infrastructure.persistence.repository.ReviewRepository;
import com.yourlifeone.finance.shared.ExpectedVersions;
import com.yourlifeone.finance.shared.Timestamps;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user's one dated banking review, replaced whole. The first save names no version and is refused
 * once a review exists; every later save names the {@code updatedAt} it replaces and is refused when the
 * review has changed since, so a stale copy never overwrites newer work.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviews;
    private final ReviewPersistenceMapper persistence;
    private final ReviewRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<FinanceReview> review(UUID userId) {
        return reviews.findById(userId).map(persistence::toDomain);
    }

    /** @param expectedUpdatedAt the {@code updatedAt} being replaced; null for the first save */
    @Transactional
    public FinanceReview save(UUID userId, FinanceReviewDraft draft, String expectedUpdatedAt) {
        var existing = reviews.lockByUserId(userId);
        requireExpectedVersion(existing, expectedUpdatedAt);
        // Older callers do not know this optional field; refreshing a review must retain user corrections.
        if (draft.expenseGroups() == null && existing.isPresent()) {
            draft = draft.toBuilder().expenseGroups(existing.get().getDocument().expenseGroups()).build();
        }
        if (existing.isPresent()) {
            var stored = existing.get().getDocument();
            var forecast = preserveForecastIncome(draft.budgetForecast(), stored.budgetForecast());
            draft = draft.toBuilder().budgetForecast(forecast)
                    .transactions(draft.transactions() == null ? stored.transactions() : draft.transactions()).build();
        }
        rules.check(draft);
        var now = Timestamps.now(clock);
        var entity = existing.orElseGet(() -> ReviewEntity.builder().userId(userId).createdAt(now).build());
        // Missing/null profiles are legacy whole-review saves: retain the stored scenarios under the same lock.
        var document = draft.loanProfiles() == null && existing.isPresent()
                ? draft.toBuilder().loanProfiles(existing.get().getDocument().loanProfiles()).build() : draft;
        rules.checkSize(document);
        entity.setDocument(document);
        entity.setAsOf(LocalDate.parse(draft.asOf()));
        entity.setUpdatedAt(now);
        return persistence.toDomain(reviews.saveAndFlush(entity));
    }

    /** A fixed command payload makes exact retries safe even after another review save. */
    @Transactional
    public FinanceReview changeProfile(UUID userId, LoanProfileChangeRequest change, String expectedUpdatedAt) {
        var existing = reviews.lockByUserId(userId);
        requireExpectedVersion(existing, expectedUpdatedAt);
        var entity = existing.orElseThrow(() -> ApiException.conflict("review_changed", "Reload the banking review."));
        var draft = entity.getDocument();
        var profiles = new ArrayList<LoanOverpaymentProfile>(draft.loanProfiles() == null ? List.of() : draft.loanProfiles());
        if ("save".equals(change.operation())) {
            var profile = change.profile();
            rules.checkProfile(profile, draft.loans());
            profiles.removeIf(saved -> saved.id().equals(profile.id()));
            if (Boolean.TRUE.equals(profile.isDefault())) {
                profiles.replaceAll(saved -> saved.toBuilder().isDefault(false).build());
            }
            profiles.add(profile);
            if (profiles.size() > FinanceReviewDraft.MAX_LOAN_PROFILES) {
                throw ApiException.badRequest("invalid_review", "At most 20 loan profiles can be saved.");
            }
        } else if (!profiles.removeIf(saved -> saved.id().equals(change.profileId()))) {
            throw ApiException.badRequest("loan_profile_not_found", "The loan profile does not exist; reload before removing it.");
        }
        var document = draft.toBuilder().loanProfiles(profiles).build();
        rules.checkSize(document);
        entity.setDocument(document);
        entity.setUpdatedAt(Timestamps.now(clock));
        return persistence.toDomain(reviews.saveAndFlush(entity));
    }

    /** A legacy forecast edit may change items without knowing the salary fields. */
    private static ReviewBudgetForecast preserveForecastIncome(ReviewBudgetForecast incoming, ReviewBudgetForecast stored) {
        if (incoming == null) return stored;
        if (stored == null) return incoming;
        return incoming.toBuilder()
                .incomePence(incoming.incomePence() == null ? stored.incomePence() : incoming.incomePence())
                .incomeBasis(incoming.incomeBasis() == null ? stored.incomeBasis() : incoming.incomeBasis()).build();
    }

    private static void requireExpectedVersion(Optional<ReviewEntity> existing, String expectedUpdatedAt) {
        if (expectedUpdatedAt == null) {
            if (existing.isPresent()) {
                throw ApiException.conflict("review_exists",
                        "A banking review already exists; reload it and save with its updatedAt.");
            }
            return;
        }
        if (existing.isEmpty() || !ExpectedVersions.matches(expectedUpdatedAt, existing.get().getUpdatedAt())) {
            throw ApiException.conflict("review_changed", "The banking review changed since it was read; reload and try again.");
        }
    }
}
