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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Salary and itemised review evidence persist without inventing dates or admitting household detail. */
@FinanceTest
class SpendingDetailsApiIntegrationTest {
    private static final String REVIEW = "/api/finance/v1/review";
    @Autowired private MockMvc mockMvc;
    @Autowired private MovableClock clock;
    private FinanceRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        requests = new FinanceRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T12:00:00Z"));
    }

    private ResultActions save(ObjectNode draft, String version) throws Exception {
        return requests.as(user, put(REVIEW).contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.saveReview(draft, version)));
    }

    private static String version(ResultActions result) {
        return FinanceSamples.read(FinanceRequests.body(result.andReturn())).at("/review/updatedAt").asString();
    }

    private ResultActions invalidPayment(Consumer<ObjectNode> change) throws Exception {
        var draft = FinanceSamples.spendingDetailsDraft();
        change.accept((ObjectNode) draft.at("/transactions/1"));
        return save(draft, null);
    }

    @Test
    void incomeAndPaymentEvidenceSurviveLegacyWritesAndAcceptExplicitZeroAndEmptyValues() throws Exception {
        var first = save(FinanceSamples.spendingDetailsDraft(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.incomePence").value(250000))
                .andExpect(jsonPath("$.review.transactions[0].date").doesNotExist())
                .andExpect(jsonPath("$.review.transactions[1].date").value("2026-08-15"))
                .andExpect(jsonPath("$.review.transactions[2].amountPence").value(-2000));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.transactions.length()").value(3));
        clock.advance(Duration.ofMinutes(1));
        var legacy = save(FinanceSamples.reviewDraft(), version(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.incomePence").value(250000))
                .andExpect(jsonPath("$.review.transactions.length()").value(3));
        var oldForecast = FinanceSamples.reviewDraft();
        oldForecast.set("budgetForecast", FinanceSamples.budgetForecast().putNull("incomePence").putNull("incomeBasis"));
        oldForecast.putNull("transactions");
        clock.advance(Duration.ofMinutes(1));
        var oldEdit = save(oldForecast, version(legacy)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.incomePence").value(250000))
                .andExpect(jsonPath("$.review.budgetForecast.incomeBasis").value("Example usual net salary estimate from the dated review."))
                .andExpect(jsonPath("$.review.transactions.length()").value(3));
        var clear = FinanceSamples.reviewDraft();
        clear.set("budgetForecast", FinanceSamples.budgetForecast().put("incomePence", 0).put("incomeBasis", ""));
        clear.putArray("transactions");
        clock.advance(Duration.ofMinutes(1));
        save(clear, version(oldEdit)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.incomePence").value(0))
                .andExpect(jsonPath("$.review.budgetForecast.incomeBasis").value(""))
                .andExpect(jsonPath("$.review.transactions").isEmpty());
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.transactions").isEmpty());
    }

    @Test
    void profileCommandsPreserveIncomeAndPaymentsAndLegacyRefreshCannotOrphanReferences() throws Exception {
        var saved = save(FinanceSamples.spendingDetailsDraft(), null).andExpect(status().isOk());
        clock.advance(Duration.ofMinutes(1));
        var changed = requests.as(user, put(REVIEW).contentType(MediaType.APPLICATION_JSON)
                        .content(FinanceSamples.profileChange(FinanceSamples.loanProfile("example-profile", true), version(saved))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.review.budgetForecast.incomePence").value(250000))
                .andExpect(jsonPath("$.review.transactions.length()").value(3));
        var noAccount = FinanceSamples.reviewDraft();
        noAccount.putArray("accounts");
        save(noAccount, version(changed)).andExpect(status().isBadRequest());
        var noSource = FinanceSamples.reviewDraft();
        noSource.putArray("sources");
        save(noSource, version(changed)).andExpect(status().isBadRequest());
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.transactions.length()").value(3));
    }

    @Test
    void invalidPaymentsAndIncomeAreRefused() throws Exception {
        invalidPayment(payment -> payment.remove("date")).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.put("date", "2026-09-01")).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.putArray("sourceIds")).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.putArray("sourceIds").add("unknown")).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.put("accountId", "unknown")).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.put("amountPence", 0)).andExpect(status().isBadRequest());
        invalidPayment(payment -> payment.put("amountPence", 1.5)).andExpect(status().isBadRequest());
        var household = FinanceSamples.spendingDetailsDraft();
        ((ObjectNode) household.at("/accounts/0")).put("ownership", "household");
        save(household, null).andExpect(status().isBadRequest());
        var duplicate = FinanceSamples.spendingDetailsDraft();
        ((ArrayNode) duplicate.get("transactions")).add(duplicate.at("/transactions/0").deepCopy());
        save(duplicate, null).andExpect(status().isBadRequest());
        var salary = FinanceSamples.spendingDetailsDraft();
        ((ObjectNode) salary.get("budgetForecast")).put("incomePence", -1);
        save(salary, null).andExpect(status().isBadRequest());
        ((ObjectNode) salary.get("budgetForecast")).put("incomePence", 1.5);
        save(salary, null).andExpect(status().isBadRequest());
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review").isEmpty());
    }
}
