package com.yourlifeone.finance.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.finance.testing.AgentApprovals;
import com.yourlifeone.finance.testing.FinanceRequests;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import com.yourlifeone.finance.testing.TestClock.MovableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app and the finance and equity agent tools: responses keep
 * the shape of their fixtures in {@code contracts/finance-service}, which the app also uses as mocks. Every
 * value is fake. Where an array mixes shapes, its first element has the optional values as null.
 */
@FinanceTest
class FinanceContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/finance-service");
    private static final String BASE = "/api/finance/v1";
    private static final String REVIEW = BASE + "/review";
    private static final String POSITIONS = BASE + "/equity/positions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    @Autowired
    private DataSource dataSource;

    private FinanceRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        requests = new FinanceRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T12:00:00Z"));
    }

    private MockHttpServletResponse call(String name, String method, String path, MockHttpServletRequestBuilder request)
            throws Exception {
        var response = requests.as(user, request).andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
        return response;
    }

    private void send(MockHttpServletRequestBuilder request) throws Exception {
        requests.as(user, request).andReturn();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String updatedAt(MockHttpServletResponse response, String pointer) throws Exception {
        return FinanceSamples.read(response.getContentAsString()).at(pointer).asString();
    }

    @Test
    void ledgerResponsesKeepTheirShape() throws Exception {
        var accounts = BASE + "/accounts";
        var transactions = BASE + "/transactions";
        var budgets = BASE + "/budgets";
        var goals = BASE + "/savings-goals";
        call("account-saved", "PUT", accounts + "/current", json(put(accounts + "/current"),
                FinanceSamples.account("Example Bank", 125000, 0)));
        send(json(put(accounts + "/savings"), FinanceSamples.account("Example Savings", 50000, 1)));
        send(json(put(transactions + "/move"), FinanceSamples.transfer("current", "savings", 10000, "2026-09-01")));
        send(json(put(transactions + "/food"), FinanceSamples.expense("current", 4599, "2026-09-20")));
        call("budget-saved", "PUT", budgets + "/groceries", json(put(budgets + "/groceries"),
                "{\"category\":\"groceries\",\"monthlyLimit\":30000}"));
        call("savings-goal-saved", "PUT", goals + "/fund", json(put(goals + "/fund"), """
                {"name":"Example fund","targetAmount":100000,"currentAmount":2500,"linkedAccountId":null,
                 "icon":"star","deadline":null,"completed":false}"""));
        send(json(put(goals + "/holiday"), """
                {"name":"Example holiday","targetAmount":50000,"currentAmount":50000,"linkedAccountId":"savings",
                 "icon":"plane","deadline":"2027-06-01","completed":true}"""));
        call("ledger", "GET", BASE + "/ledger", get(BASE + "/ledger"));

        call("transaction-saved", "PUT", transactions + "/pay", json(put(transactions + "/pay"),
                FinanceSamples.income("current", 250000, "2026-09-25")));
        call("transaction-invalid", "PUT", transactions + "/bad", json(put(transactions + "/bad"),
                FinanceSamples.transaction("transfer", 100, "transfer", "current", null, "2026-09-25")));
        call("transaction-deleted", "DELETE", transactions + "/pay", delete(transactions + "/pay"));
        call("account-deleted", "DELETE", accounts + "/savings", delete(accounts + "/savings"));
        call("budget-deleted", "DELETE", budgets + "/groceries", delete(budgets + "/groceries"));
        call("savings-goal-deleted", "DELETE", goals + "/fund", delete(goals + "/fund"));
    }

    @Test
    void reviewResponsesKeepTheirShape() throws Exception {
        call("review-empty", "GET", REVIEW, get(REVIEW));
        var saved = call("review-saved", "PUT", REVIEW, json(put(REVIEW),
                FinanceSamples.saveReview(FinanceSamples.reviewDraft(), null)));
        call("review", "GET", REVIEW, get(REVIEW));
        clock.advance(Duration.ofMinutes(1));
        send(json(put(REVIEW), FinanceSamples.saveReview(FinanceSamples.reviewDraft(), updatedAt(saved, "/review/updatedAt"))));
        call("review-changed", "PUT", REVIEW, json(put(REVIEW),
                FinanceSamples.saveReview(FinanceSamples.reviewDraft(), updatedAt(saved, "/review/updatedAt"))));
    }

    @Test
    void loanProfilesExtendTheReviewWithoutChangingLegacyResponses() throws Exception {
        var saved = requests.as(user, json(put(REVIEW), FinanceSamples.saveReview(FinanceSamples.reviewDraft(), null)))
                .andReturn().getResponse();
        clock.advance(Duration.ofMinutes(1));
        call("review-loan-profile-saved", "PUT", REVIEW, json(put(REVIEW), FinanceSamples.profileChange(
                FinanceSamples.loanProfile("example-profile", true), updatedAt(saved, "/review/updatedAt"))));
        call("review-loan-profiles", "GET", REVIEW, get(REVIEW));
    }

    @Test
    void expenseGroupResponsesKeepTheirShape() throws Exception {
        var draft = FinanceSamples.reviewDraft();
        var groups = draft.putArray("expenseGroups");
        groups.addObject().put("label", "Rent").put("group", "non-discretionary");
        groups.addObject().put("label", "Groceries").put("group", "necessary");
        groups.addObject().put("label", "Streaming").put("group", "discretionary");
        call("review-expense-groups", "PUT", REVIEW, json(put(REVIEW), FinanceSamples.saveReview(draft, null)));
    }

    @Test
    void budgetForecastResponsesKeepTheirShape() throws Exception {
        var draft = FinanceSamples.reviewDraft();
        draft.set("budgetForecast", FinanceSamples.budgetForecast());
        call("review-budget-forecast", "PUT", REVIEW, json(put(REVIEW), FinanceSamples.saveReview(draft, null)));
    }

    @Test
    void spendingDetailsResponsesKeepTheirShape() throws Exception {
        call("review-spending-details", "PUT", REVIEW, json(put(REVIEW),
                FinanceSamples.saveReview(FinanceSamples.spendingDetailsDraft(), null)));
    }

    @Test
    void equityResponsesKeepTheirShape() throws Exception {
        var path = POSITIONS + "/example";
        var saved = call("position-saved", "PUT", path, json(put(path),
                FinanceSamples.savePosition(FinanceSamples.positionDraft("Example Co"), null)));
        call("equity-positions", "GET", POSITIONS + "?query=example&limit=50&offset=0",
                get(POSITIONS).param("query", "example").param("limit", "50").param("offset", "0"));
        call("equity-position", "GET", path, get(path));
        call("position-changed", "PUT", path, json(put(path),
                FinanceSamples.savePosition(FinanceSamples.positionDraft("Example Co"), "2026-01-01T00:00:00Z")));
        var version = updatedAt(saved, "/updatedAt");
        call("position-deleted", "DELETE", path + "?expectedUpdatedAt=" + version,
                delete(path).param("expectedUpdatedAt", version));
    }

    @Test
    void refusalsKeepTheirShape() throws Exception {
        new AgentApprovals(dataSource).approve(AgentApprovals.FINANCE, user, "finance-agent", false);
        var refused = requests.asAgent(user, "finance-agent", get(BASE + "/ledger")).andReturn().getResponse();
        CONTRACT.check("agent-not-approved", "GET", BASE + "/ledger", refused.getStatus(), refused.getContentAsString());

        var limited = UUID.randomUUID();
        var response = requests.as(limited, get(REVIEW)).andReturn().getResponse();
        for (int attempt = 0; attempt < 400 && response.getStatus() != 429; attempt++) {
            response = requests.as(limited, get(REVIEW)).andReturn().getResponse();
        }
        CONTRACT.check("rate-limited", "GET", REVIEW, response.getStatus(), response.getContentAsString());
    }
}
