package com.yourlifeone.life.health.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The health domain's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HealthProperties.class)
public class HealthConfiguration {
    /** How often fast food happens: entries dated in the last 7 days, by how the meal went. */
    @Bean
    BusinessGauge fastFoodThisWeekGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.health.fastfood.entries.last7days", "Fast-food meals in the last 7 days",
                () -> jdbc.sql("""
                        SELECT rating, count(*) AS total
                        FROM health.fast_food_entry
                        WHERE entry_date > current_date - 7 AND entry_date <= current_date
                        GROUP BY rating""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "rating", rs.getString("rating")))
                        .list());
    }
}
