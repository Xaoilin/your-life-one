package com.yourlifeone.knowledge.lifestyle.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The lifestyle tracker's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LifestyleProperties.class)
public class LifestyleConfiguration {
    /** Where the user stands: tracked items by column and status (both small, configured sets). */
    @Bean
    BusinessGauge lifestyleItemsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.lifestyle.items", "Lifestyle tracker items by column and status",
                () -> jdbc.sql("""
                        SELECT type, status, count(*) AS total
                        FROM lifestyle.item
                        GROUP BY type, status""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "type", rs.getString("type"), "status", rs.getString("status")))
                        .list());
    }
}
