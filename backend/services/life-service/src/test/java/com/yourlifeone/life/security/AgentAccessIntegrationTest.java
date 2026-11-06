package com.yourlifeone.life.security;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.testing.LifeRequests;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI agents (tokens with a client_id) reach only the domains the user approved them for, using the
 * approval tables the React app keeps in Supabase; the app's own tokens are unaffected.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class AgentAccessIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private LifeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new LifeRequests(mockMvc);
        var jdbc = JdbcClient.create(dataSource);
        for (var table : new String[] {"yourlifeone_inventory_oauth_clients", "yourlifeone_employment_oauth_clients"}) {
            jdbc.sql("CREATE TABLE IF NOT EXISTS public." + table
                    + " (user_id uuid, client_id text, revoked_at timestamptz)").update();
        }
    }

    private void approve(String table, UUID user, String client, boolean revoked) {
        JdbcClient.create(dataSource).sql("INSERT INTO public." + table + " (user_id, client_id, revoked_at)"
                        + " VALUES (?, ?, " + (revoked ? "now()" : "NULL") + ")")
                .params(user, client).update();
    }

    @Test
    void anApprovedAgentReachesOnlyItsApprovedDomain() throws Exception {
        var user = UUID.randomUUID();
        approve("yourlifeone_inventory_oauth_clients", user, "inventory-agent", false);

        requests.asAgent(user, "inventory-agent", get("/api/life/v1/inventory/search")).andExpect(status().isOk());
        requests.asAgent(user, "inventory-agent", get("/api/life/v1/jobs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("agent_not_approved"));
        // Trips and health have no agent tools, so no agent may use them.
        requests.asAgent(user, "inventory-agent", get("/api/life/v1/trips")).andExpect(status().isForbidden());
        requests.asAgent(user, "inventory-agent", get("/api/life/v1/health/fast-food")).andExpect(status().isForbidden());
    }

    @Test
    void unapprovedRevokedAndOtherUsersAgentsAreRefused() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        approve("yourlifeone_employment_oauth_clients", user, "revoked-agent", true);
        approve("yourlifeone_employment_oauth_clients", other, "jobs-agent", false);

        requests.asAgent(user, "unknown-agent", get("/api/life/v1/jobs")).andExpect(status().isForbidden());
        requests.asAgent(user, "revoked-agent", get("/api/life/v1/jobs")).andExpect(status().isForbidden());
        requests.asAgent(user, "jobs-agent", get("/api/life/v1/jobs")).andExpect(status().isForbidden());
        requests.asAgent(other, "jobs-agent", get("/api/life/v1/jobs")).andExpect(status().isOk());
        requests.as(user, get("/api/life/v1/jobs")).andExpect(status().isOk());
    }
}
