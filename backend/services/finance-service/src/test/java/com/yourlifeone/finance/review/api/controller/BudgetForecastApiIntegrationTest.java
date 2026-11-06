package com.yourlifeone.finance.review.api.controller;

import com.yourlifeone.finance.review.domain.ReviewBudgetForecast;
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

/** Saved forecasts share the review's account ownership, version and queued write boundary. */
@FinanceTest
class BudgetForecastApiIntegrationTest {
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

    private static String updatedAt(ResultActions result) {
        return FinanceSamples.read(FinanceRequests.body(result.andReturn())).at("/review/updatedAt").asString();
    }

    private ResultActions invalidForecast(Consumer<ObjectNode> change) throws Exception {
        var forecast = FinanceSamples.budgetForecast();
        change.accept(forecast);
        var draft = FinanceSamples.reviewDraft();
        draft.set("budgetForecast", forecast);
        return save(draft, null);
    }

    @Test
    void savedForecastSurvivesReadsAndLegacyRefreshesAndCanBeExplicitlyEmptied() throws Exception {
        var draft = FinanceSamples.reviewDraft();
        draft.set("budgetForecast", FinanceSamples.budgetForecast());
        draft.putArray("expenseGroups").addObject().put("label", "Groceries").put("group", "necessary");
        draft.putArray("loanProfiles").add(FinanceSamples.loanProfile("example-profile", true));
        var first = save(draft, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.baselineMonth").value("2026-09"))
                .andExpect(jsonPath("$.review.budgetForecast.items[0].amountPence").value(100000));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.budgetForecast.items.length()").value(3));

        clock.advance(Duration.ofMinutes(1));
        var refreshed = save(FinanceSamples.reviewDraft(), updatedAt(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.items[0].id").value("rent"))
                .andExpect(jsonPath("$.review.expenseGroups[0].group").value("necessary"))
                .andExpect(jsonPath("$.review.loanProfiles[0].id").value("example-profile"));
        clock.advance(Duration.ofMinutes(1));
        var nullRefresh = save(FinanceSamples.reviewDraft().putNull("budgetForecast"), updatedAt(refreshed))
                .andExpect(status().isOk()).andExpect(jsonPath("$.review.budgetForecast.items.length()").value(3));
        save(draft, updatedAt(first)).andExpect(status().isConflict());

        var empty = FinanceSamples.reviewDraft();
        var forecast = FinanceSamples.budgetForecast();
        forecast.putArray("items");
        empty.set("budgetForecast", forecast);
        clock.advance(Duration.ofMinutes(1));
        save(empty, updatedAt(nullRefresh)).andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.items").isEmpty())
                .andExpect(jsonPath("$.review.expenseGroups[0].group").value("necessary"))
                .andExpect(jsonPath("$.review.loanProfiles[0].id").value("example-profile"));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.budgetForecast.items").isEmpty());
    }

    @Test
    void narrowLoanProfileCommandsPreserveTheSavedForecast() throws Exception {
        var draft = FinanceSamples.reviewDraft();
        draft.set("budgetForecast", FinanceSamples.budgetForecast());
        var saved = save(draft, null).andExpect(status().isOk());
        clock.advance(Duration.ofMinutes(1));
        requests.as(user, put(REVIEW).contentType(MediaType.APPLICATION_JSON)
                        .content(FinanceSamples.profileChange(FinanceSamples.loanProfile("example-profile", true), updatedAt(saved))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.budgetForecast.items[0].amountPence").value(100000))
                .andExpect(jsonPath("$.review.loanProfiles[0].id").value("example-profile"));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.budgetForecast.items[0].amountPence").value(100000));
    }

    @Test
    void invalidForecastsAreRefusedWithoutSavingAnyReview() throws Exception {
        invalidForecast(forecast -> forecast.put("baselineMonth", "2026-13")).andExpect(status().isBadRequest());
        invalidForecast(forecast -> ((ObjectNode) forecast.at("/items/0")).put("amountPence", -1))
                .andExpect(status().isBadRequest());
        invalidForecast(forecast -> ((ObjectNode) forecast.at("/items/0")).put("amountPence", 1.5))
                .andExpect(status().isBadRequest());
        invalidForecast(forecast -> ((ObjectNode) forecast.at("/items/0")).put("group", "unknown"))
                .andExpect(status().isBadRequest());
        invalidForecast(forecast -> ((ArrayNode) forecast.get("items")).add(forecast.at("/items/0").deepCopy()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Duplicate forecast item IDs."));
        invalidForecast(forecast -> {
            var items = forecast.putArray("items");
            for (int index = 0; index <= ReviewBudgetForecast.MAX_ITEMS; index++) {
                var item = (ObjectNode) FinanceSamples.budgetForecast().at("/items/0");
                item.put("id", "example-" + index);
                items.add(item);
            }
        }).andExpect(status().isBadRequest());
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review").isEmpty());
    }
}
