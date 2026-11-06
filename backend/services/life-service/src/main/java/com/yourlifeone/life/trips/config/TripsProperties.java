package com.yourlifeone.life.trips.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed values and limits of trips ({@code app.life.trips.*}).
 *
 * @param bookingBudgetCategories the budget category each booking kind counts under, e.g. stay to rent
 * @param defaultCurrency budget currency of a trip that names none
 */
@ConfigurationProperties("app.life.trips")
public record TripsProperties(
        List<String> statuses,
        List<String> transportModes,
        List<String> budgetCategories,
        List<String> budgetStatuses,
        Map<String, String> bookingBudgetCategories,
        String defaultCurrency,
        int maxLegs) {
    public TripsProperties {
        statuses = List.copyOf(statuses);
        transportModes = List.copyOf(transportModes);
        budgetCategories = List.copyOf(budgetCategories);
        budgetStatuses = List.copyOf(budgetStatuses);
        bookingBudgetCategories = Map.copyOf(bookingBudgetCategories);
        if (!budgetCategories.containsAll(bookingBudgetCategories.values())) {
            throw new IllegalArgumentException("app.life.trips.booking-budget-categories must use budget categories");
        }
    }
}
