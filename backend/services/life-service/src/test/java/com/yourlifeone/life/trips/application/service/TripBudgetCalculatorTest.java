package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.life.trips.config.TripsProperties;
import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.Trip;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripBudgetCalculatorTest {
    private final TripBudgetCalculator calculator = new TripBudgetCalculator(new TripsProperties(
            List.of("planning"), List.of("flight"), List.of("transport", "food", "rent", "other"),
            List.of("planned", "paid"), Map.of("transport", "transport", "stay", "rent"), "GBP", 100));

    @Test
    void addsCostedBookingsAndManualItemsAndCountsUncostedBookings() {
        var trip = Trip.builder().id("t").budgetTotal(100_000).build();
        var bookings = List.of(
                Booking.builder().id("f").kind("transport").budgetAmount(40_000L).budgetStatus("paid").build(),
                Booking.builder().id("s").kind("stay").build());
        var entries = List.of(BudgetEntry.builder().id("e").category("food").amount(70_000).status("planned").build());

        var summary = calculator.summarize(trip, bookings, entries);

        assertEquals(110_000, summary.forecastTotal());
        assertEquals(40_000, summary.paidTotal());
        assertEquals(-10_000, summary.remaining());
        assertEquals(1, summary.uncostedBookingCount());
        var rent = summary.byCategory().stream().filter(category -> category.category().equals("rent")).findFirst()
                .orElseThrow();
        assertEquals(1, rent.count());
        assertEquals(1, rent.uncostedCount());
        assertEquals(0, rent.forecast());
    }
}
