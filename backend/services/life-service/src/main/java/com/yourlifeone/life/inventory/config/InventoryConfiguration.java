package com.yourlifeone.life.inventory.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The inventory domain's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InventoryProperties.class)
public class InventoryConfiguration {
    /** The shopping list: needs still to buy or on order, by broad category. */
    @Bean
    BusinessGauge openNeedsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.inventory.needs.open", "Shopping-list needs still needed or ordered",
                () -> jdbc.sql("""
                        SELECT coalesce(category, 'uncategorised') AS category, status, count(*) AS total
                        FROM inventory.need
                        WHERE status IN ('needed', 'ordered')
                        GROUP BY 1, 2""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "category", rs.getString("category"), "status", rs.getString("status")))
                        .list());
    }

    /** What is owned: items in stock (not archived), by broad category. */
    @Bean
    BusinessGauge ownedItemsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.inventory.items.owned", "Owned items not archived",
                () -> jdbc.sql("""
                        SELECT category, count(*) AS total FROM inventory.item
                        WHERE archived_at IS NULL GROUP BY category""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "category", rs.getString("category")))
                        .list());
    }
}
