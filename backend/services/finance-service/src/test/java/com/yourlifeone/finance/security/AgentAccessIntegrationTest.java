package com.yourlifeone.finance.security;

import com.yourlifeone.finance.testing.AgentApprovals;
import com.yourlifeone.finance.testing.FinanceRequests;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI agents (tokens with a client_id) reach only the part the user approved them for: Finance approval
 * reaches the banking review, Equity approval the positions, and the ledger refuses every agent. The app's
 * own tokens are unaffected.
 */
@FinanceTest
class AgentAccessIntegrationTest {
    private static final String BASE = "/api/finance/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private FinanceRequests requests;
    private AgentApprovals approvals;

    @BeforeEach
    void setUp() {
        requests = new FinanceRequests(mockMvc);
        approvals = new AgentApprovals(dataSource);
    }

    @Test
    void aFinanceAgentReachesOnlyTheReview() throws Exception {
        var user = UUID.randomUUID();
        approvals.approve(AgentApprovals.FINANCE, user, "finance-agent", false);

        requests.asAgent(user, "finance-agent", get(BASE + "/review")).andExpect(status().isOk());
        requests.asAgent(user, "finance-agent", put(BASE + "/review").contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.saveReview(FinanceSamples.reviewDraft(), null))).andExpect(status().isOk());
        requests.asAgent(user, "finance-agent", get(BASE + "/equity/positions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("agent_not_approved"));
        requests.asAgent(user, "finance-agent", get(BASE + "/ledger")).andExpect(status().isForbidden());
    }

    @Test
    void anEquityAgentReachesOnlyThePositions() throws Exception {
        var user = UUID.randomUUID();
        approvals.approve(AgentApprovals.EQUITY, user, "equity-agent", false);

        requests.asAgent(user, "equity-agent", get(BASE + "/equity/positions")).andExpect(status().isOk());
        requests.asAgent(user, "equity-agent", put(BASE + "/equity/positions/example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.savePosition(FinanceSamples.positionDraft("Example Co"), null)))
                .andExpect(status().isOk());
        requests.asAgent(user, "equity-agent", get(BASE + "/review")).andExpect(status().isForbidden());
        requests.asAgent(user, "equity-agent", put(BASE + "/accounts/a").contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.account("Example Bank", 0, 0))).andExpect(status().isForbidden());
    }

    @Test
    void unapprovedRevokedAndOtherUsersAgentsAreRefused() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        approvals.approve(AgentApprovals.FINANCE, user, "revoked-agent", true);
        approvals.approve(AgentApprovals.FINANCE, other, "their-agent", false);

        requests.asAgent(user, "unknown-agent", get(BASE + "/review")).andExpect(status().isForbidden());
        requests.asAgent(user, "revoked-agent", get(BASE + "/review")).andExpect(status().isForbidden());
        requests.asAgent(user, "their-agent", get(BASE + "/review")).andExpect(status().isForbidden());
        requests.asAgent(other, "their-agent", get(BASE + "/review")).andExpect(status().isOk());
        requests.as(user, get(BASE + "/ledger")).andExpect(status().isOk());
        requests.as(user, get(BASE + "/equity/positions")).andExpect(status().isOk());
    }
}
