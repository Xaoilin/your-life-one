package com.yourlifeone.finance.review.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.finance.testing.FinanceRequests;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import com.yourlifeone.finance.testing.TestClock.MovableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Profile commands cross the real queue and database, including exact replay and competing writers. */
@FinanceTest
class LoanProfilesApiIntegrationTest {
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

    private ResultActions send(String body, String key) throws Exception {
        return requests.as(user, put(REVIEW).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(IdempotencyKeys.HEADER, key));
    }

    private String version(ResultActions result) {
        return FinanceSamples.read(FinanceRequests.body(result.andReturn())).at("/review/updatedAt").asString();
    }

    private String create() throws Exception {
        return version(send(FinanceSamples.saveReview(FinanceSamples.reviewDraft(), null), UUID.randomUUID().toString())
                .andExpect(status().isOk()));
    }

    @Test
    void profilesAreAccountOwnedDefaultIsUniqueAndLegacyOmissionPreservesThem() throws Exception {
        var initial = create();
        clock.advance(Duration.ofMinutes(1));
        var one = version(send(FinanceSamples.profileChange(FinanceSamples.loanProfile("one", true), initial), "one")
                .andExpect(status().isOk()));
        clock.advance(Duration.ofMinutes(1));
        var two = version(send(FinanceSamples.profileChange(FinanceSamples.loanProfile("two", true), one), "two")
                .andExpect(jsonPath("$.review.loanProfiles[0].isDefault").value(false))
                .andExpect(jsonPath("$.review.loanProfiles[1].isDefault").value(true)));
        requests.as(UUID.randomUUID(), get(REVIEW)).andExpect(jsonPath("$.review").isEmpty());
        var legacy = FinanceSamples.reviewDraft();
        clock.advance(Duration.ofMinutes(1));
        var retained = version(send(FinanceSamples.saveReview(legacy, two), "legacy")
                .andExpect(jsonPath("$.review.loanProfiles.length()").value(2)));
        legacy.putNull("loanProfiles");
        clock.advance(Duration.ofMinutes(1));
        var retainedNull = version(send(FinanceSamples.saveReview(legacy, retained), "legacy-null")
                .andExpect(jsonPath("$.review.loanProfiles.length()").value(2)));
        legacy.putArray("loanProfiles");
        clock.advance(Duration.ofMinutes(1));
        send(FinanceSamples.saveReview(legacy, retainedNull), "clear").andExpect(status().isOk())
                .andExpect(jsonPath("$.review.loanProfiles").doesNotExist());
    }

    @Test
    void exactRetryAfterNewerWritesReturnsTheOriginalReceiptAndStaleNewWritesConflict() throws Exception {
        var initial = create();
        clock.advance(Duration.ofMinutes(1));
        var command = FinanceSamples.profileChange(FinanceSamples.loanProfile("one", false), initial);
        var first = send(command, "stable-retry").andExpect(status().isOk());
        var original = FinanceRequests.body(first.andReturn());
        var savedVersion = version(first);
        clock.advance(Duration.ofMinutes(1));
        send(FinanceSamples.profileChange(FinanceSamples.loanProfile("two", true), savedVersion), "second")
                .andExpect(status().isOk());
        var replay = send(command, "stable-retry").andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
        assertEquals(FinanceSamples.read(original), FinanceSamples.read(FinanceRequests.body(replay.andReturn())));
        send(command, "new-stale-write").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("review_changed"));
        requests.as(user, get(REVIEW)).andExpect(jsonPath("$.review.loanProfiles.length()").value(2));
    }

    @Test
    void competingProfileCommandsCannotOverwriteEachOtherAndRemovalNeedsConfirmation() throws Exception {
        var initial = create();
        clock.advance(Duration.ofMinutes(1));
        var results = FinanceRequests.concurrently(2, number -> send(FinanceSamples.profileChange(
                FinanceSamples.loanProfile("profile-" + number, true), initial), "parallel-" + number));
        var statuses = results.stream().map(result -> result.getResponse().getStatus()).sorted().toList();
        assertEquals(List.of(200, 409), statuses);
        var current = requests.as(user, get(REVIEW)).andReturn();
        var document = FinanceSamples.read(FinanceRequests.body(current)).get("review");
        var body = FinanceSamples.JSON.createObjectNode();
        body.put("expectedUpdatedAt", document.get("updatedAt").asString());
        body.putObject("loanProfileChange").put("operation", "remove")
                .put("profileId", document.get("loanProfiles").get(0).get("id").asString()).put("confirmed", false);
        send(body.toString(), "unconfirmed").andExpect(status().isBadRequest());
        ((ObjectNode) body.get("loanProfileChange")).put("confirmed", true);
        clock.advance(Duration.ofMinutes(1));
        send(body.toString(), "remove").andExpect(status().isOk()).andExpect(jsonPath("$.review.loanProfiles").doesNotExist());
        send(body.toString(), "remove").andExpect(status().isOk()).andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
    }

    @Test
    void validatesProfileBoundsReferencesAndDefaultAndAllowsRepaidLoans() throws Exception {
        var invalid = List.<Consumer<ObjectNode>>of(
                profile -> profile.put("name", " "), profile -> profile.put("firstPaymentMonth", "2026-13"),
                profile -> profile.put("reviewAsOf", "2026-02-30"), profile -> profile.put("selectedLoanId", "missing"),
                profile -> loan(profile).put("loanId", "unknown"), profile -> loan(profile).put("balancePence", -1),
                profile -> loan(profile).put("monthlyPaymentPence", 0), profile -> loan(profile).put("monthlyExtraPence", 1.5),
                profile -> loan(profile).put("annualRatePercent", 101), profile -> loan(profile).put("sharePercent", 101),
                profile -> loan(profile).put("extraStartsAfterMonths", 1200), profile -> loan(profile).put("repaymentMode", "other"),
                profile -> ((ArrayNode) profile.get("loans")).add(profile.get("loans").get(0).deepCopy()));
        for (var change : invalid) {
            var draft = FinanceSamples.reviewDraft();
            var profile = FinanceSamples.loanProfile("bad", true);
            change.accept(profile);
            draft.putArray("loanProfiles").add(profile);
            send(FinanceSamples.saveReview(draft, null), UUID.randomUUID().toString()).andExpect(status().isBadRequest());
        }
        var draft = FinanceSamples.reviewDraft();
        var profiles = draft.putArray("loanProfiles");
        profiles.add(FinanceSamples.loanProfile("same", true)).add(FinanceSamples.loanProfile("same", false));
        send(FinanceSamples.saveReview(draft, null), "duplicate").andExpect(status().isBadRequest());
        profiles.removeAll().add(FinanceSamples.loanProfile("one", true)).add(FinanceSamples.loanProfile("two", true));
        send(FinanceSamples.saveReview(draft, null), "defaults").andExpect(status().isBadRequest());
        profiles.removeAll();
        for (var index = 0; index < 21; index++) profiles.add(FinanceSamples.loanProfile("profile-" + index, false));
        send(FinanceSamples.saveReview(draft, null), "too-many").andExpect(status().isBadRequest());
        var repaid = FinanceSamples.loanProfile("repaid", true);
        loan(repaid).put("loanId", "loan-2");
        profiles.removeAll().add(repaid);
        send(FinanceSamples.saveReview(draft, null), "valid-repaid").andExpect(status().isOk());
    }

    private static ObjectNode loan(ObjectNode profile) {
        return (ObjectNode) profile.get("loans").get(0);
    }
}
