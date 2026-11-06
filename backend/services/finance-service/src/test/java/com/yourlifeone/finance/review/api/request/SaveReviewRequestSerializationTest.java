package com.yourlifeone.finance.review.api.request;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.finance.testing.FinanceSamples;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** New absent fields cannot change the fingerprint of an old caller's exact retry. */
class SaveReviewRequestSerializationTest {
    @Test
    void legacyRequestSerializationDoesNotAddNewNullFieldsButRetainsExplicitClears() {
        var original = FinanceSamples.read(FinanceSamples.saveReview(FinanceSamples.reviewDraft(), null));
        var request = FinanceSamples.JSON.treeToValue(original, SaveReviewRequest.class);
        var serialized = FinanceSamples.JSON.valueToTree(request);
        assertEquals(IdempotencyKeys.fingerprint("finance.save-review", original),
                IdempotencyKeys.fingerprint("finance.save-review", serialized));
        assertFalse(serialized.has("loanProfileChange"));
        assertFalse(serialized.get("review").has("loanProfiles"));
        assertFalse(serialized.get("review").has("expenseGroups"));
        assertFalse(serialized.get("review").has("budgetForecast"));
        assertFalse(serialized.get("review").has("transactions"));
        var clearing = FinanceSamples.reviewDraft();
        clearing.putArray("loanProfiles");
        clearing.putArray("expenseGroups");
        clearing.putArray("transactions");
        var emptyForecast = FinanceSamples.budgetForecast();
        emptyForecast.putArray("items");
        clearing.set("budgetForecast", emptyForecast);
        var clear = FinanceSamples.JSON.treeToValue(FinanceSamples.read(FinanceSamples.saveReview(clearing, null)), SaveReviewRequest.class);
        assertTrue(FinanceSamples.JSON.valueToTree(clear).get("review").get("loanProfiles").isArray());
        assertTrue(FinanceSamples.JSON.valueToTree(clear).get("review").get("expenseGroups").isArray());
        assertTrue(FinanceSamples.JSON.valueToTree(clear).at("/review/budgetForecast/items").isArray());
        assertTrue(FinanceSamples.JSON.valueToTree(clear).at("/review/transactions").isArray());
    }
    @Test
    void olderForecastWritesDoNotGainNewNullSalaryFields() {
        var draft = FinanceSamples.reviewDraft();
        draft.set("budgetForecast", FinanceSamples.budgetForecast());
        var original = FinanceSamples.read(FinanceSamples.saveReview(draft, null));
        var request = FinanceSamples.JSON.treeToValue(original, SaveReviewRequest.class);
        var serialized = FinanceSamples.JSON.valueToTree(request);
        assertEquals(IdempotencyKeys.fingerprint("finance.save-review", original),
                IdempotencyKeys.fingerprint("finance.save-review", serialized));
        assertFalse(serialized.at("/review/budgetForecast").has("incomePence"));
        assertFalse(serialized.at("/review/budgetForecast").has("incomeBasis"));
    }
}
