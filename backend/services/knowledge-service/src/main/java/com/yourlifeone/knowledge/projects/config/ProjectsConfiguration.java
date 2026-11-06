package com.yourlifeone.knowledge.projects.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The project catalogue's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProjectsProperties.class)
public class ProjectsConfiguration {
    /** What is being built: projects by status and kind (both small, configured sets). */
    @Bean
    BusinessGauge projectsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.projects.projects", "Projects by status and kind",
                () -> jdbc.sql("""
                        SELECT status, kind, count(*) AS total
                        FROM projects.project
                        GROUP BY status, kind""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "status", rs.getString("status"), "kind", rs.getString("kind")))
                        .list());
    }

    /** How much is documented: wiki pages written or changed in the last 30 days. */
    @Bean
    BusinessGauge projectPagesUpdatedGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.projects.pages.updated.last30days",
                "Project wiki pages written or changed in the last 30 days",
                () -> jdbc.sql("""
                        SELECT count(*) AS total
                        FROM projects.page
                        WHERE updated_at > now() - interval '30 days'""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total")))
                        .list());
    }
}
