package com.yourlifeone.life.trips.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The trips domain's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TripsProperties.class)
public class TripsConfiguration {
    /** Trips not yet over (ending today or later), by status. */
    @Bean
    BusinessGauge upcomingTripsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.trips.upcoming", "Trips ending today or later, by status",
                () -> jdbc.sql("""
                        SELECT status, count(*) AS total FROM trips.trip
                        WHERE coalesce(end_date, start_date, current_date) >= current_date
                        GROUP BY status""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "status", rs.getString("status")))
                        .list());
    }

    /**
     * Forecast spend of trips not yet over, by budget category and currency, in major units: manual
     * budget items plus costed bookings under their kind's category.
     */
    @Bean
    BusinessGauge upcomingBudgetGauge(JdbcClient jdbc, TripsProperties properties) {
        return new BusinessGauge("yourlife.trips.budget.forecast", "Forecast spend of upcoming trips, in major units",
                () -> Stream.concat(manualBudget(jdbc).stream(), bookingBudget(jdbc, properties).stream())
                        .collect(Collectors.groupingBy(row -> row.tags(),
                                Collectors.summingDouble(BusinessGauge.Row::value)))
                        .entrySet().stream().map(entry -> new BusinessGauge.Row(entry.getKey(), entry.getValue()))
                        .toList());
    }

    private static List<BusinessGauge.Row> manualBudget(JdbcClient jdbc) {
        return jdbc.sql("""
                        SELECT b.category, t.budget_currency AS currency, sum(b.amount) / 100.0 AS total
                        FROM trips.budget_entry b
                        JOIN trips.trip t ON t.user_id = b.user_id AND t.id = b.trip_id
                        WHERE coalesce(t.end_date, t.start_date, current_date) >= current_date
                        GROUP BY b.category, t.budget_currency""")
                .query((rs, row) -> BusinessGauge.Row.of(rs.getDouble("total"),
                        "category", rs.getString("category"), "currency", rs.getString("currency")))
                .list();
    }

    private static List<BusinessGauge.Row> bookingBudget(JdbcClient jdbc, TripsProperties properties) {
        return jdbc.sql("""
                        SELECT b.kind, t.budget_currency AS currency, sum(b.budget_amount) / 100.0 AS total
                        FROM trips.booking b
                        JOIN trips.trip t ON t.user_id = b.user_id AND t.id = b.trip_id
                        WHERE b.budget_amount IS NOT NULL
                          AND coalesce(t.end_date, t.start_date, current_date) >= current_date
                        GROUP BY b.kind, t.budget_currency""")
                .query((rs, row) -> BusinessGauge.Row.of(rs.getDouble("total"),
                        "category", properties.bookingBudgetCategories().getOrDefault(rs.getString("kind"), "other"),
                        "currency", rs.getString("currency")))
                .list();
    }
}
