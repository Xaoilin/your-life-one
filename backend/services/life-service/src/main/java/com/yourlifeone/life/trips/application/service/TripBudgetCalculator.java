package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.life.trips.config.TripsProperties;
import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.BudgetSummary;
import com.yourlifeone.life.trips.domain.CategoryBudget;
import com.yourlifeone.life.trips.domain.Trip;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A trip's budget: every booking counts under its kind's category (uncosted until it has an amount)
 * and every manual item under its own. Forecast adds all amounts, paid only paid ones, and remaining is
 * the trip's total minus the forecast.
 */
@Component
@RequiredArgsConstructor
public class TripBudgetCalculator {
    private static final String PAID = "paid";

    private final TripsProperties properties;

    public BudgetSummary summarize(Trip trip, List<Booking> bookings, List<BudgetEntry> entries) {
        var ledger = Stream.concat(
                bookings.stream().map(booking -> new Line(bookingCategory(booking), booking.budgetAmount(),
                        booking.budgetStatus() == null ? "planned" : booking.budgetStatus())),
                entries.stream().map(entry -> new Line(entry.category(), entry.amount(), entry.status()))).toList();
        var forecast = forecast(ledger);
        var byCategory = properties.budgetCategories().stream().map(category -> {
            var lines = ledger.stream().filter(line -> line.category().equals(category)).toList();
            return new CategoryBudget(category, forecast(lines), paid(lines), lines.size(),
                    (int) lines.stream().filter(line -> line.amount() == null).count());
        }).toList();
        var uncosted = (int) bookings.stream().filter(booking -> booking.budgetAmount() == null).count();
        return new BudgetSummary(forecast, paid(ledger), trip.budgetTotal() - forecast, uncosted, byCategory);
    }

    private String bookingCategory(Booking booking) {
        return properties.bookingBudgetCategories().getOrDefault(booking.kind(), "other");
    }

    private static long forecast(List<Line> lines) {
        return lines.stream().filter(line -> line.amount() != null).mapToLong(Line::amount).sum();
    }

    private static long paid(List<Line> lines) {
        return lines.stream().filter(line -> line.amount() != null && PAID.equals(line.status()))
                .mapToLong(Line::amount).sum();
    }

    /** One costed or uncosted line of the budget. */
    private record Line(String category, Long amount, String status) {
    }
}
