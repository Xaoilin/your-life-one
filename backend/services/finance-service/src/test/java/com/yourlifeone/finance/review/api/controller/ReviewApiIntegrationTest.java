package com.yourlifeone.finance.review.api.controller;

import com.yourlifeone.finance.testing.FinanceRequests;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import com.yourlifeone.finance.testing.TestClock.MovableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The dated banking review: first save, optimistic concurrency and the draft's validation. */
@FinanceTest
class ReviewApiIntegrationTest {
    private static final String REVIEW = "/api/finance/v1/review";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    private FinanceRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        requests = new FinanceRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T12:00:00Z"));
    }

    private ResultActions save(ObjectNode draft, String expectedUpdatedAt) throws Exception {
        return requests.as(user, put(REVIEW).contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.saveReview(draft, expectedUpdatedAt)));
    }

    private String updatedAt(ResultActions result) {
        return FinanceSamples.read(FinanceRequests.body(result.andReturn())).at("/review/updatedAt").asString();
    }

    private ResultActions saveChanged(Consumer<ObjectNode> change) throws Exception {
        var draft = FinanceSamples.reviewDraft();
        change.accept(draft);
        return save(draft, null);
    }

    @Test
    void theFirstSaveCreatesTheReviewAndLaterSavesNameTheVersionTheyReplace() throws Exception {
        requests.as(user, get(REVIEW)).andExpect(status().isOk()).andExpect(jsonPath("$.review").isEmpty());

        var first = save(FinanceSamples.reviewDraft(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.id").value("current"))
                .andExpect(jsonPath("$.review.loans[0].balancePence").isEmpty())
                .andExpect(jsonPath("$.review.loans[1].aprPercent").value(9.9))
                .andExpect(jsonPath("$.review.createdAt").value("2026-09-27T12:00:00Z"));
        var version = updatedAt(first);

        save(FinanceSamples.reviewDraft(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("review_exists"));

        clock.advance(Duration.ofMinutes(1));
        var changed = FinanceSamples.reviewDraft().put("asOf", "2026-09-27");
        var second = save(changed, version)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.asOf").value("2026-09-27"))
                .andExpect(jsonPath("$.review.createdAt").value("2026-09-27T12:00:00Z"));
        assertEquals("2026-09-27T12:01:00Z", updatedAt(second));

        save(FinanceSamples.reviewDraft(), version)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("review_changed"));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.asOf").value("2026-09-27"));
    }

    @Test
    void aVersionForAReviewThatDoesNotExistIsRefused() throws Exception {
        save(FinanceSamples.reviewDraft(), "2026-09-27T12:00:00Z")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("review_changed"));
    }

    @Test
    void rulesAcrossFieldsAreEnforced() throws Exception {
        saveChanged(draft -> ((ObjectNode) draft.get("months").get(0)).put("netPence", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_review"))
                .andExpect(jsonPath("$.message").value("Monthly net must equal income minus outflow."));
        saveChanged(draft -> ((ObjectNode) draft.get("loans").get(1)).put("id", "loan-1"))
                .andExpect(jsonPath("$.message").value("Duplicate loan IDs."));
        saveChanged(draft -> ((ArrayNode) draft.get("months")).add(draft.get("months").get(0).deepCopy()))
                .andExpect(jsonPath("$.message").value("Duplicate months."));
        saveChanged(draft -> ((ObjectNode) draft.get("loans").get(1)).putArray("sourceIds").add("nowhere"))
                .andExpect(jsonPath("$.message").value("Loan references an unknown source."));
        saveChanged(draft -> ((ObjectNode) draft.get("loans").get(1)).putNull("balanceAsOf"))
                .andExpect(jsonPath("$.message").value("Balances and settlements require their own dates."));
        saveChanged(draft -> ((ObjectNode) draft.get("loans").get(1)).put("startDate", "2027-01-01"))
                .andExpect(jsonPath("$.message").value("Loan end precedes its start."));
        saveChanged(draft -> ((ObjectNode) draft.get("coverage")).put("completeThrough", "2026-09"))
                .andExpect(jsonPath("$.message").value("Coverage dates are inconsistent."));
    }

    @Test
    void expenseGroupsPersistAcrossReadsAndOlderRefreshesUntilExplicitlyCleared() throws Exception {
        var draft = FinanceSamples.reviewDraft();
        draft.putArray("expenseGroups").addObject().put("label", "Groceries").put("group", "necessary");
        var saved = save(draft, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.expenseGroups[0].group").value("necessary"));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.expenseGroups[0].label").value("Groceries"));

        clock.advance(Duration.ofMinutes(1));
        var olderRefresh = save(FinanceSamples.reviewDraft(), updatedAt(saved)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.expenseGroups[0].group").value("necessary"))
                .andExpect(jsonPath("$.review.months[0].outflowPence").value(200000));
        clock.advance(Duration.ofMinutes(1));
        var nullRefresh = save(FinanceSamples.reviewDraft().putNull("expenseGroups"), updatedAt(olderRefresh))
                .andExpect(status().isOk()).andExpect(jsonPath("$.review.expenseGroups[0].group").value("necessary"));

        clock.advance(Duration.ofMinutes(1));
        var cleared = FinanceSamples.reviewDraft();
        cleared.putArray("expenseGroups");
        save(cleared, updatedAt(nullRefresh)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.expenseGroups").isEmpty());
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.expenseGroups").isEmpty());
    }

    @Test
    void expenseGroupsRejectUnknownGroupsBlankLabelsAndDuplicateLabels() throws Exception {
        saveChanged(draft -> draft.putArray("expenseGroups").addObject()
                .put("label", "Groceries").put("group", "unknown"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> draft.putArray("expenseGroups").addObject()
                .put("label", " ").put("group", "necessary"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> {
            var groups = draft.putArray("expenseGroups");
            groups.addObject().put("label", "Groceries").put("group", "necessary");
            groups.addObject().put("label", " groceries ").put("group", "discretionary");
        }).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_review"))
                .andExpect(jsonPath("$.message").value("Duplicate expense group labels."));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review").isEmpty());
    }

    @Test
    void fieldsAreValidated() throws Exception {
        saveChanged(draft -> draft.put("currency", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> draft.put("asOf", "2026-02-30")).andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> ((ObjectNode) draft.get("sources").get(0)).put("url", "https://user:secret@example.com/x"))
                .andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> ((ObjectNode) draft.get("months").get(0)).put("month", "2026-13"))
                .andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> ((ObjectNode) draft.get("loans").get(0)).put("status", "forgotten"))
                .andExpect(jsonPath("$.code").value("invalid_request"));
        saveChanged(draft -> draft.remove("budget")).andExpect(jsonPath("$.code").value("invalid_request"));

        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review").isEmpty());
    }
}
