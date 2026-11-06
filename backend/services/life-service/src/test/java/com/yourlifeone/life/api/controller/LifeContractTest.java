package com.yourlifeone.life.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app and its agent tools: responses keep the shape of
 * their fixtures in {@code contracts/life-service}, which the app also uses as mocks.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class LifeContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/life-service");
    private static final String BASE = "/api/life/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private final UUID user = UUID.randomUUID();

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request) throws Exception {
        call(name, method, path, request, null);
    }

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request, String clientId)
            throws Exception {
        var response = mockMvc.perform(request.with(jwt().jwt(token -> {
            token.subject(user.toString());
            if (clientId != null) token.claim("client_id", clientId);
        }))).andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void healthJournalResponsesKeepTheirShape() throws Exception {
        var path = BASE + "/health/fast-food";
        call("fast-food-entry-saved", "PUT", path + "/entry-1", json(put(path + "/entry-1"), """
                {"venue":"McDonalds","date":"2026-04-20","order":"Big Arch meal","rating":"bad",
                 "symptoms":["nauseous"],"notes":"Not worth it"}"""));
        call("fast-food-entries", "GET", path, get(path));
        call("fast-food-entry-invalid", "PUT", path + "/entry-2", json(put(path + "/entry-2"), """
                {"venue":"KFC","date":"2026-04-20","rating":"great","symptoms":[]}"""));
    }

    @Test
    void jobTrackerResponsesKeepTheirShape() throws Exception {
        var applications = BASE + "/jobs/applications";
        var one = applications + "/acme-engineer";
        call("application-added", "POST", applications, json(post(applications), """
                {"id":"acme-engineer","company":"Acme","role":"Engineer","url":"https://jobs.example/1",
                 "status":"applied","applicationDate":"2026-09-01","remoteRegion":"uk","remoteStatus":"confirmed",
                 "remoteEvidence":"UK remote in the listing","compensation":"£600/day","history":[
                  {"id":"h1","kind":"application","date":"2026-09-01","summary":"Applied","details":"Via site",
                   "evidenceUrl":"https://mail.example/1"}]}"""));
        call("application-duplicate", "POST", applications, json(post(applications), """
                {"id":"acme-engineer","company":"Acme","role":"Engineer"}"""));
        call("application-updated", "PATCH", one, json(patch(one), """
                {"status":"interview","nextAction":"Prepare","nextActionDate":"2026-09-30","clear":["url"]}"""));
        call("application-history-added", "POST", one + "/history", json(post(one + "/history"), """
                {"kind":"contact","summary":"Recruiter replied","evidenceUrl":"https://mail.example/2"}"""));
        call("application-changed", "PATCH", one, json(patch(one), """
                {"status":"offer","expectedUpdatedAt":"2020-01-01T00:00:00Z"}"""));
        call("jobs", "GET", BASE + "/jobs", get(BASE + "/jobs"));
        call("applications-page", "GET", applications, get(applications).param("query", "acme"));
        call("application", "GET", one, get(one));
        call("application-removed", "DELETE", one, delete(one).param("confirm", "true"));
    }

    @Test
    void inventoryResponsesKeepTheirShape() throws Exception {
        var inventory = BASE + "/inventory";
        call("item-saved", "PUT", inventory + "/items/rail", json(put(inventory + "/items/rail"), """
                {"name":"Rail slides","category":"component","subcategory":"mechanical_hardware",
                 "imageUrl":"https://m.media-amazon.com/images/I/rail.jpg","trackingMode":"counted","quantity":2,
                 "unit":"pcs","lowStockThreshold":1,"brand":"Acme","model":"R-550","location":"Garage",
                 "dimensions":{"length":550,"width":45,"height":12.7,"unit":"mm"},"specifications":{"mount":"Side"},
                 "condition":"new","tags":["desk"],"notes":"","projectCatalogKeys":["magnus-desk"]}"""));
        call("items-saved", "POST", inventory + "/items", json(post(inventory + "/items"), """
                {"items":[{"id":"rail","notes":"Checked"},{"id":"pla","name":"PLA","quantity":3,"unit":"spools"}]}"""));
        call("need-saved", "PUT", inventory + "/needs/screws", json(put(inventory + "/needs/screws"), """
                {"name":"M4 screws","category":"component","subcategory":"mechanical_hardware",
                 "imageUrl":"https://m.media-amazon.com/images/I/screws.jpg","linkedItemId":"rail",
                 "projectCatalogKey":"magnus-desk","requiredQuantity":20,"unit":"pcs",
                 "dimensions":{"length":10,"unit":"mm"},"specifications":{"thread":"M4"},"priority":"high",
                 "status":"ordered","notes":""}"""));
        call("need-open", "PUT", inventory + "/needs/glue", json(put(inventory + "/needs/glue"), """
                {"name":"Wood glue","requiredQuantity":1,"unit":"bottle","specifications":{},"priority":"low",
                 "status":"needed","notes":""}"""));
        call("inventory", "GET", inventory, get(inventory));
        call("inventory-search", "GET", inventory + "/search", get(inventory + "/search").param("query", ""));
        call("stock-check", "GET", inventory + "/check", get(inventory + "/check").param("name", "rail")
                .param("requiredQuantity", "1"));
        call("need-acquired", "POST", inventory + "/needs/screws/acquire", post(inventory + "/needs/screws/acquire"));
        call("item-archived", "POST", inventory + "/items/pla/archive", post(inventory + "/items/pla/archive"));
        call("item-invalid", "PUT", inventory + "/items/bad", json(put(inventory + "/items/bad"), """
                {"name":"Bad","category":"tool","subcategory":"filament","trackingMode":"counted","quantity":1,
                 "unit":"pcs","condition":"new"}"""));
    }

    @Test
    void tripResponsesKeepTheirShape() throws Exception {
        var trips = BASE + "/trips";
        call("trip-plan-created", "POST", trips + "/plans", json(post(trips + "/plans"), """
                {"id":"saudi","trip":{"name":"Trip to Saudi","summary":"Family","notes":"","status":"planning",
                  "budgetCurrency":"GBP","budgetTotal":500000},
                 "legs":[{"id":"riyadh","country":"Saudi Arabia","city":"Riyadh","startDate":"2026-06-17",
                          "endDate":"2026-07-02"}],
                 "bookings":[{"id":"flight","tripId":"saudi","legId":"riyadh","kind":"transport","mode":"flight",
                   "title":"Flight to Saudi","fromLabel":"UK","toLabel":"Riyadh","departAt":"2026-06-17T09:00",
                   "arriveAt":"2026-06-17T17:00","budgetAmount":100000,"budgetStatus":"paid","budgetDate":"2026-06-01",
                   "provider":"BA","confirmationCode":"ABC123","link":"https://ba.example/abc","notes":""}]}"""));
        call("trip-saved", "PUT", trips + "/saudi", json(put(trips + "/saudi"), """
                {"name":"Trip to Saudi","summary":"Family","notes":"Visa","status":"booked","budgetCurrency":"GBP",
                 "budgetTotal":600000,"startDate":"2026-06-17","endDate":"2026-07-02"}"""));
        call("leg-saved", "PUT", trips + "/legs/bahrain", json(put(trips + "/legs/bahrain"), """
                {"tripId":"saudi","country":"Bahrain","city":"Manama","startDate":"2026-07-02","endDate":"2026-07-15"}"""));
        call("leg-moved", "POST", trips + "/legs/bahrain/move", json(post(trips + "/legs/bahrain/move"),
                "{\"direction\":-1}"));
        call("itinerary-item-saved", "PUT", trips + "/itinerary-items/souq", json(put(trips + "/itinerary-items/souq"), """
                {"tripId":"saudi","legId":"riyadh","date":"2026-06-18","title":"Souq","startTime":"10:00",
                 "endTime":"12:00","location":"Old town","notes":"","sortOrder":0}"""));
        call("booking-saved", "PUT", trips + "/bookings/flat", json(put(trips + "/bookings/flat"), """
                {"tripId":"saudi","legId":"bahrain","kind":"stay","title":"Flat","propertyName":"2 bedroom flat",
                 "address":"1 Road","city":"Manama","country":"Bahrain","checkInDate":"2026-07-02",
                 "checkOutDate":"2026-07-15","budgetAmount":100000,"budgetStatus":"planned","notes":""}"""));
        call("budget-entry-saved", "PUT", trips + "/budget-entries/food", json(put(trips + "/budget-entries/food"), """
                {"tripId":"saudi","title":"Food","category":"food","amount":25000,"status":"planned",
                 "date":"2026-06-17","notes":""}"""));
        call("trips", "GET", trips, get(trips));
        call("leg-deleted", "DELETE", trips + "/legs/bahrain", delete(trips + "/legs/bahrain"));
        call("trip-deleted", "DELETE", trips + "/saudi", delete(trips + "/saudi"));
    }

    @Test
    void refusedAgentsAndRateLimitedRequestsKeepTheirShape() throws Exception {
        JdbcClient.create(dataSource).sql("CREATE TABLE IF NOT EXISTS public.yourlifeone_employment_oauth_clients"
                + " (user_id uuid, client_id text, revoked_at timestamptz)").update();
        call("agent-not-approved", "GET", BASE + "/jobs/applications", get(BASE + "/jobs/applications"),
                "unapproved-agent");
        var limited = UUID.randomUUID();
        var response = mockMvc.perform(get(BASE + "/jobs").with(jwt().jwt(token -> token.subject(limited.toString()))))
                .andReturn().getResponse();
        for (int request = 0; request < 200 && response.getStatus() != 429; request++) {
            response = mockMvc.perform(get(BASE + "/jobs").with(jwt().jwt(token -> token.subject(limited.toString()))))
                    .andReturn().getResponse();
        }
        CONTRACT.check("rate-limited", "GET", BASE + "/jobs", response.getStatus(), response.getContentAsString());
    }
}
