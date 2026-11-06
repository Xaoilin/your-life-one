package com.yourlifeone.knowledge.knowledge.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The knowledge base's settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeConfiguration {
    /** How much is written down: topics and entries in the knowledge base. */
    @Bean
    BusinessGauge knowledgeRecordsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.knowledge.records", "Knowledge-base topics and entries",
                () -> jdbc.sql("""
                        SELECT 'topic' AS kind, count(*) AS total FROM knowledge.topic
                        UNION ALL
                        SELECT 'entry', count(*) FROM knowledge.entry""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "kind", rs.getString("kind")))
                        .list());
    }

    /** What the knowledge base rests on: cited sources by type (Quran, hadith, scholarly, other). */
    @Bean
    BusinessGauge knowledgeSourcesGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.knowledge.sources", "Sources cited by knowledge-base entries, by type",
                () -> jdbc.sql("""
                        SELECT source ->> 'type' AS type, count(*) AS total
                        FROM knowledge.entry, jsonb_array_elements(sources) source
                        GROUP BY source ->> 'type'""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "type", rs.getString("type")))
                        .list());
    }

    /** How the knowledge base is growing: entries written or changed in the last 30 days. */
    @Bean
    BusinessGauge knowledgeEntriesUpdatedGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.knowledge.entries.updated.last30days",
                "Knowledge-base entries written or changed in the last 30 days",
                () -> jdbc.sql("""
                        SELECT count(*) AS total
                        FROM knowledge.entry
                        WHERE updated_at > now() - interval '30 days'""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total")))
                        .list());
    }
}
