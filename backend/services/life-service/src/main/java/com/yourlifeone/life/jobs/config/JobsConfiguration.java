package com.yourlifeone.life.jobs.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The jobs domain's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JobsProperties.class)
public class JobsConfiguration {
    /** Where the job search stands: applications by stage (lead, applied, interview, offer, closed...). */
    @Bean
    BusinessGauge applicationsByStageGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.jobs.applications", "Job applications by stage",
                () -> jdbc.sql("SELECT status, count(*) AS total FROM jobs.application GROUP BY status")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "status", rs.getString("status")))
                        .list());
    }
}
