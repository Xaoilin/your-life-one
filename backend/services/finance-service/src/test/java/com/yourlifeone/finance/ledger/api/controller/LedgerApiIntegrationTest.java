package com.yourlifeone.finance.ledger.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.finance.testing.FinanceRequests;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import com.yourlifeone.finance.testing.TestClock.MovableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The ledger through the full stack: balances owned by the service, deletes, budgets, goals and limits. */
@FinanceTest
class LedgerApiIntegrationTest {
    private static final String BASE = "/api/finance/v1";
    private static final String DATE = "2026-09-20";

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

    private ResultActions save(String path, String body) throws Exception {
        return requests.as(user, put(BASE + path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions remove(String path) throws Exception {
        return requests.as(user, delete(BASE + path));
    }

    private void account(String id, long balance) throws Exception {
        save("/accounts/" + id, FinanceSamples.account("Example Bank " + id, balance, 0)).andExpect(status().isOk());
    }

    private long balance(String id) throws Exception {
        var ledger = FinanceSamples.read(FinanceRequests.body(requests.as(user, get(BASE + "/ledger")).andReturn()));
        for (var account : ledger.get("accounts")) {
            if (account.get("id").asString().equals(id)) return account.get("balance").asLong();
        }
        throw new AssertionError("No account " + id);
    }

    @Test
    void savingAnAccountSetsItsFieldsAndKeepsWhenItWasCreated() throws Exception {
        account("a1", 10000);
        clock.advance(Duration.ofMinutes(5));

        save("/accounts/a1", FinanceSamples.account("Example Savings", 2500, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Example Savings"))
                .andExpect(jsonPath("$.balance").value(2500))
                .andExpect(jsonPath("$.sortOrder").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-09-27T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-27T12:05:00Z"));
    }

    @Test
    void transactionsMoveBalancesAndReplacingOrDeletingRevertsTheirEffect() throws Exception {
        account("a", 10000);
        account("b", 0);

        save("/transactions/pay", FinanceSamples.income("a", 500, DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.toAccountId").isEmpty())
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].balance").value(10500));
        save("/transactions/food", FinanceSamples.expense("a", 200, DATE))
                .andExpect(jsonPath("$.accounts[0].balance").value(10300));
        save("/transactions/move", FinanceSamples.transfer("a", "b", 1000, DATE))
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].balance").value(9300))
                .andExpect(jsonPath("$.accounts[1].balance").value(1000));

        // Replacing reverts the old effect and applies the new one.
        save("/transactions/move", FinanceSamples.transfer("a", "b", 300, DATE))
                .andExpect(jsonPath("$.transaction.amount").value(300));
        assertEquals(10000, balance("a"));
        assertEquals(300, balance("b"));
        save("/transactions/food", FinanceSamples.income("a", 200, DATE));
        assertEquals(10400, balance("a"));
        save("/transactions/move", FinanceSamples.expense("b", 100, DATE))
                .andExpect(jsonPath("$.accounts.length()").value(2));
        assertEquals(10700, balance("a"));
        assertEquals(-100, balance("b"));

        remove("/transactions/pay")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay"))
                .andExpect(jsonPath("$.accounts[0].balance").value(10200));
        remove("/transactions/pay").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("transaction_not_found"));
    }

    @Test
    void deletingAnAccountRemovesTransactionsOnEitherSideAndUnlinksItsGoals() throws Exception {
        account("keep", 5000);
        account("gone", 0);
        save("/transactions/t-transfer", FinanceSamples.transfer("keep", "gone", 1000, DATE));
        save("/transactions/t-income", FinanceSamples.income("gone", 400, DATE));
        save("/transactions/t-expense", FinanceSamples.expense("keep", 100, DATE));
        save("/savings-goals/holiday", """
                {"name":"Example holiday","targetAmount":50000,"currentAmount":1000,"linkedAccountId":"gone",
                 "icon":"plane","deadline":"2027-06-01","completed":false}""").andExpect(status().isOk());

        remove("/accounts/gone")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gone"))
                .andExpect(jsonPath("$.removedTransactionIds[0]").value("t-income"))
                .andExpect(jsonPath("$.removedTransactionIds[1]").value("t-transfer"));

        requests.as(user, get(BASE + "/ledger"))
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].id").value("t-expense"))
                .andExpect(jsonPath("$.savingsGoals[0].linkedAccountId").isEmpty());
        // The remaining account keeps its balance as it was: removing the account does not undo transfers.
        assertEquals(3900, balance("keep"));
        remove("/accounts/gone").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("account_not_found"));
    }

    @Test
    void eachExpenseCategoryHasOneBudget() throws Exception {
        save("/budgets/food", "{\"category\":\"groceries\",\"monthlyLimit\":30000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(30000));
        save("/budgets/food-2", "{\"category\":\"groceries\",\"monthlyLimit\":100}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("budget_exists"));
        save("/budgets/food", "{\"category\":\"transport\",\"monthlyLimit\":30000}").andExpect(status().isOk());
        save("/budgets/food-2", "{\"category\":\"groceries\",\"monthlyLimit\":100}").andExpect(status().isOk());
        save("/budgets/pay", "{\"category\":\"salary\",\"monthlyLimit\":100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_category"));

        remove("/budgets/food-2").andExpect(status().isOk()).andExpect(jsonPath("$.id").value("food-2"));
        remove("/budgets/food-2").andExpect(status().isNotFound());
    }

    @Test
    void theServiceStampsWhenAGoalIsCompleted() throws Exception {
        var goal = """
                {"name":"Example fund","targetAmount":10000,"currentAmount":10000,"linkedAccountId":null,
                 "icon":"star","deadline":null,"completed":%s}""";
        save("/savings-goals/fund", goal.formatted("false")).andExpect(jsonPath("$.completedAt").isEmpty());
        save("/savings-goals/fund", goal.formatted("true"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").value("2026-09-27T12:00:00Z"));
        clock.advance(Duration.ofHours(1));
        save("/savings-goals/fund", goal.formatted("true")).andExpect(jsonPath("$.completedAt").value("2026-09-27T12:00:00Z"));
        save("/savings-goals/fund", goal.formatted("false")).andExpect(jsonPath("$.completedAt").isEmpty());

        save("/savings-goals/other", goal.formatted("false").replace("\"linkedAccountId\":null", "\"linkedAccountId\":\"missing\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("account_not_found"));
        remove("/savings-goals/fund").andExpect(status().isOk()).andExpect(jsonPath("$.id").value("fund"));
    }

    @Test
    void invalidWritesAreRefusedWithoutChangingBalances() throws Exception {
        account("a", 1000);
        save("/transactions/t1", FinanceSamples.transaction("transfer", 100, "transfer", "a", null, DATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_to_account"));
        save("/transactions/t1", FinanceSamples.transaction("transfer", 100, "transfer", "a", "a", DATE))
                .andExpect(jsonPath("$.code").value("invalid_to_account"));
        save("/transactions/t1", FinanceSamples.transaction("income", 100, "salary", "a", "a", DATE))
                .andExpect(jsonPath("$.code").value("invalid_to_account"));
        save("/transactions/t1", FinanceSamples.income("a", 0, DATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        save("/transactions/t1", FinanceSamples.transaction("expense", 100, "yachts", "a", null, DATE))
                .andExpect(jsonPath("$.code").value("invalid_category"));
        save("/transactions/t1", FinanceSamples.transaction("gift", 100, "salary", "a", null, DATE))
                .andExpect(jsonPath("$.code").value("invalid_type"));
        save("/transactions/t1", FinanceSamples.income("a", 100, "2026-02-30"))
                .andExpect(jsonPath("$.code").value("invalid_request"));
        save("/transactions/t1", FinanceSamples.transfer("a", "missing", 100, DATE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("account_not_found"));
        save("/accounts/b", FinanceSamples.account("Example Bank", 0, 0).replace("GBP", "gbp"))
                .andExpect(jsonPath("$.code").value("invalid_request"));
        save("/accounts/b", FinanceSamples.account("Example Bank", 0, 0).replace("current", "crypto"))
                .andExpect(jsonPath("$.code").value("invalid_type"));
        save("/accounts/" + "x".repeat(257), FinanceSamples.account("Example Bank", 0, 0))
                .andExpect(jsonPath("$.code").value("invalid_id"));

        assertEquals(1000, balance("a"));
        requests.as(user, get(BASE + "/ledger")).andExpect(jsonPath("$.transactions.length()").value(0));
    }

    @Test
    void aRetriedWriteIsAppliedOnce() throws Exception {
        account("a", 0);
        var request = put(BASE + "/transactions/once").contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.income("a", 700, DATE)).header(IdempotencyKeys.HEADER, "retry-1");

        requests.as(user, request).andExpect(status().isOk());
        requests.as(user, put(BASE + "/transactions/once").contentType(MediaType.APPLICATION_JSON)
                        .content(FinanceSamples.income("a", 700, DATE)).header(IdempotencyKeys.HEADER, "retry-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));

        assertEquals(700, balance("a"));
    }

    @Test
    void parallelWritesOnOneAccountKeepItsBalanceConsistent() throws Exception {
        account("shared", 0);
        var results = FinanceRequests.concurrently(20, number -> save("/transactions/p" + number,
                number % 2 == 0 ? FinanceSamples.income("shared", 100, DATE) : FinanceSamples.expense("shared", 30, DATE)));

        assertTrue(results.stream().allMatch(result -> result.getResponse().getStatus() == 200));
        assertEquals(10 * 100 - 10 * 30, balance("shared"));
    }

    /**
     * The bucket is per user across every endpoint, so it is emptied with a cheap read (one row by key), not
     * the whole ledger.
     */
    @Test
    void aUserOverTheRateLimitGets429() throws Exception {
        var limited = UUID.randomUUID();
        var status = 200;
        String retryAfter = null;
        for (int request = 0; request < 400 && status != 429; request++) {
            var response = requests.as(limited, get(BASE + "/review")).andReturn().getResponse();
            status = response.getStatus();
            retryAfter = response.getHeader("Retry-After");
        }
        assertEquals(429, status);
        assertTrue(retryAfter != null && Integer.parseInt(retryAfter) > 0);
    }
}
