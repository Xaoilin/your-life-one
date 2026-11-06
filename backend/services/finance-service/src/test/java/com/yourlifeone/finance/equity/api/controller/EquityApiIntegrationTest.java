package com.yourlifeone.finance.equity.api.controller;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Equity positions: create, search, optimistic concurrency on edits and deletes, and validation. */
@FinanceTest
class EquityApiIntegrationTest {
    private static final String POSITIONS = "/api/finance/v1/equity/positions";

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

    private ResultActions save(String id, JsonNode draft, String expectedUpdatedAt) throws Exception {
        return requests.as(user, put(POSITIONS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(FinanceSamples.savePosition(draft, expectedUpdatedAt)));
    }

    private String updatedAt(ResultActions result) {
        return FinanceSamples.read(FinanceRequests.body(result.andReturn())).get("updatedAt").asString();
    }

    @Test
    void positionsAreCreatedOnceAndEditedFromTheLatestVersion() throws Exception {
        var created = save("example", FinanceSamples.positionDraft("  Example Co  "), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("example"))
                .andExpect(jsonPath("$.company").value("Example Co"))
                .andExpect(jsonPath("$.grants[0].nextVest").isEmpty())
                .andExpect(jsonPath("$.grants[1].nextVest.quantity").value(25))
                .andExpect(jsonPath("$.scenario.withholdingRate").value(0.45));
        var version = updatedAt(created);
        save("example", FinanceSamples.positionDraft("Example Co"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("position_exists"));

        clock.advance(Duration.ofMinutes(1));
        var edited = FinanceSamples.positionDraft("Example Co").put("ownedShares", 150);
        var newVersion = updatedAt(save("example", edited, version)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedShares").value(150))
                .andExpect(jsonPath("$.createdAt").value("2026-09-27T12:00:00Z")));
        save("example", edited, version)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("position_changed"));
        save("missing", edited, version).andExpect(status().isNotFound());

        requests.as(user, get(POSITIONS + "/example")).andExpect(jsonPath("$.updatedAt").value(newVersion));
        requests.as(user, get(POSITIONS + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("position_not_found"));

        requests.as(user, delete(POSITIONS + "/example").param("expectedUpdatedAt", version))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("position_changed"));
        requests.as(user, delete(POSITIONS + "/example")).andExpect(status().isBadRequest());
        requests.as(user, delete(POSITIONS + "/example").param("expectedUpdatedAt", newVersion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("example"));
        requests.as(user, get(POSITIONS + "/example")).andExpect(status().isNotFound());
    }

    @Test
    void searchMatchesPartOfTheCompanyIgnoringCaseAndPages() throws Exception {
        save("a", FinanceSamples.positionDraft("Example Co"), null).andExpect(status().isOk());
        save("b", FinanceSamples.positionDraft("Another Example Ltd"), null).andExpect(status().isOk());
        save("c", FinanceSamples.positionDraft("Fake 100% Holdings"), null).andExpect(status().isOk());

        requests.as(user, get(POSITIONS).param("query", "EXAMPLE"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.positions[0].id").value("a"))
                .andExpect(jsonPath("$.positions[1].id").value("b"));
        requests.as(user, get(POSITIONS).param("limit", "1").param("offset", "1"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].id").value("b"));
        requests.as(user, get(POSITIONS).param("query", "0%")).andExpect(jsonPath("$.total").value(1));
        requests.as(user, get(POSITIONS).param("query", "x_m")).andExpect(jsonPath("$.total").value(0));
        requests.as(user, get(POSITIONS).param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_page"));
    }

    @Test
    void positionsAreValidated() throws Exception {
        var badRate = FinanceSamples.positionDraft("Example Co");
        ((ObjectNode) badRate.get("scenario")).put("withholdingRate", 2);
        save("bad", badRate, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_request"));
        save("bad", FinanceSamples.positionDraft("   "), null).andExpect(jsonPath("$.code").value("invalid_request"));
        var badUrl = FinanceSamples.positionDraft("Example Co");
        ((ObjectNode) badUrl.get("sources").get(0)).put("url", "ftp://example.com/plan");
        save("bad", badUrl, null).andExpect(jsonPath("$.code").value("invalid_request"));
        var badVest = FinanceSamples.positionDraft("Example Co");
        ((ObjectNode) badVest.get("grants").get(1).get("nextVest")).put("date", "2026-1-1");
        save("bad", badVest, null).andExpect(jsonPath("$.code").value("invalid_request"));

        requests.as(user, get(POSITIONS)).andExpect(jsonPath("$.total").value(0));
    }
}
