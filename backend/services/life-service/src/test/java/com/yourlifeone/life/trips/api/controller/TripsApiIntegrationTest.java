package com.yourlifeone.life.trips.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Trips through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class TripsApiIntegrationTest {
    private static final String TRIPS = "/api/life/v1/trips";
    private static final String PLAN = """
            {"id":"saudi","trip":{"name":"Trip to Saudi","status":"planning","budgetCurrency":"gbp","budgetTotal":500000},
             "legs":[{"id":"riyadh","country":"Saudi Arabia","city":"Riyadh","startDate":"2026-06-17","endDate":"2026-07-02"},
                     {"id":"bahrain","country":"Bahrain","city":"Manama","startDate":"2026-07-02","endDate":"2026-07-15"}],
             "bookings":[{"id":"flight","tripId":"saudi","legId":"riyadh","kind":"transport","mode":"flight",
                          "title":"Flight to Saudi","fromLabel":"UK","toLabel":"Riyadh","departAt":"2026-06-17T09:00",
                          "budgetAmount":100000,"budgetStatus":"paid"},
                         {"id":"flat","tripId":"saudi","legId":"bahrain","kind":"stay","title":"Flat",
                          "propertyName":"2 bedroom flat","city":"Manama","country":"Bahrain",
                          "checkInDate":"2026-07-02","checkOutDate":"2026-07-15"}]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private LifeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new LifeRequests(mockMvc);
    }

    private void plan(UUID user) throws Exception {
        requests.as(user, post(TRIPS + "/plans").contentType(MediaType.APPLICATION_JSON).content(PLAN))
                .andExpect(status().isOk());
    }

    @Test
    void aPlanCreatesTheTripDatedByItsLegsWithItsBookingsAndBudget() throws Exception {
        var user = UUID.randomUUID();

        requests.as(user, post(TRIPS + "/plans").contentType(MediaType.APPLICATION_JSON).content(PLAN))
                .andExpect(jsonPath("$.trip.startDate").value("2026-06-17"))
                .andExpect(jsonPath("$.trip.endDate").value("2026-07-15"))
                .andExpect(jsonPath("$.trip.budgetCurrency").value("GBP"))
                .andExpect(jsonPath("$.legs[1].sortOrder").value(1))
                .andExpect(jsonPath("$.bookings.length()").value(2))
                .andExpect(jsonPath("$.bookings[1].arriveAt").doesNotExist())
                .andExpect(jsonPath("$.trip.budget.forecastTotal").value(100000))
                .andExpect(jsonPath("$.trip.budget.paidTotal").value(100000))
                .andExpect(jsonPath("$.trip.budget.remaining").value(400000))
                .andExpect(jsonPath("$.trip.budget.uncostedBookingCount").value(1))
                .andExpect(jsonPath("$.trip.budget.byCategory[?(@.category == 'rent')].uncostedCount").value(1));
        requests.as(user, get(TRIPS)).andExpect(jsonPath("$.trips.length()").value(1))
                .andExpect(jsonPath("$.legs.length()").value(2));
    }

    @Test
    void routeChangesRefitTheTripDatesAndRenumberLegs() throws Exception {
        var user = UUID.randomUUID();
        plan(user);

        requests.as(user, put(TRIPS + "/legs/dubai").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tripId":"saudi","country":"UAE","city":"Dubai","startDate":"2026-07-15","endDate":"2026-07-20"}"""))
                .andExpect(jsonPath("$.trip.endDate").value("2026-07-20"))
                .andExpect(jsonPath("$.legs[2].sortOrder").value(2));
        requests.as(user, post(TRIPS + "/legs/dubai/move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":-1}"))
                .andExpect(jsonPath("$.legs[1].id").value("dubai"))
                .andExpect(jsonPath("$.trip.endDate").value("2026-07-15"));
        // Removing a leg removes its bookings and closes the gap in the route.
        requests.as(user, delete(TRIPS + "/legs/riyadh"))
                .andExpect(jsonPath("$.legs.length()").value(2))
                .andExpect(jsonPath("$.legs[0].id").value("dubai"))
                .andExpect(jsonPath("$.legs[0].sortOrder").value(0))
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.trip.startDate").value("2026-07-15"));
    }

    @Test
    void partsStayWithTheirTripAndBudgetItemsCountInTheBudget() throws Exception {
        var user = UUID.randomUUID();
        plan(user);

        requests.as(user, put(TRIPS + "/budget-entries/food").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tripId":"saudi","title":"Food","category":"food","amount":25000,"status":"planned",
                         "date":"2026-06-17"}"""))
                .andExpect(jsonPath("$.trip.budget.forecastTotal").value(125000))
                .andExpect(jsonPath("$.trip.budget.byCategory[?(@.category == 'food')].forecast").value(25000));
        requests.as(user, put(TRIPS + "/itinerary-items/souq").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tripId":"saudi","legId":"riyadh","date":"2026-06-18","title":"Souq","startTime":"10:00"}"""))
                .andExpect(jsonPath("$.itineraryItems[0].startTime").value("10:00"));
        requests.as(user, put(TRIPS + "/itinerary-items/souq").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tripId":"other","legId":"riyadh","title":"Souq"}"""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("trip_changed"));
        requests.as(user, put(TRIPS + "/bookings/train").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tripId":"saudi","kind":"transport","mode":"rocket","title":"Train"}"""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_mode"));
    }

    @Test
    void deletingATripDeletesEveryPart() throws Exception {
        var user = UUID.randomUUID();
        plan(user);

        requests.as(user, delete(TRIPS + "/saudi")).andExpect(status().isNoContent());
        var jdbc = JdbcClient.create(dataSource);
        for (var table : new String[] {"trip", "leg", "booking", "itinerary_item", "budget_entry"}) {
            assertEquals(0, jdbc.sql("SELECT count(*) FROM trips." + table + " WHERE user_id = ?").param(user)
                    .query(Integer.class).single());
        }
    }

    @Test
    void concurrentDuplicatePlansCreateOneTrip() throws Exception {
        var user = UUID.randomUUID();
        var sameKey = LifeRequests.concurrently(8, attempt -> requests.as(user, post(TRIPS + "/plans")
                .contentType(MediaType.APPLICATION_JSON).content(PLAN).header(IdempotencyKeys.HEADER, "plan-1")));
        var differentKeys = LifeRequests.concurrently(8, attempt -> requests.as(user, post(TRIPS + "/plans")
                .contentType(MediaType.APPLICATION_JSON).content(PLAN).header(IdempotencyKeys.HEADER, "plan-" + attempt)));

        for (var response : sameKey) assertEquals(200, response.getResponse().getStatus());
        for (var response : differentKeys) assertEquals(200, response.getResponse().getStatus());
        assertEquals(2, JdbcClient.create(dataSource).sql("SELECT count(*) FROM trips.leg WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }
}
