package com.yourlifeone.calendar.api.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Actual statement counters from the isolated Testcontainers PostgreSQL, never a hosted database. */
final class CalendarSyncEvidence {
    private final JdbcClient jdbc;

    CalendarSyncEvidence(DataSource dataSource) {
        jdbc = JdbcClient.create(dataSource);
        jdbc.sql("CREATE EXTENSION IF NOT EXISTS pg_stat_statements").update();
    }

    void reset() {
        jdbc.sql("SELECT pg_stat_statements_reset()").query(String.class).single();
    }

    List<Map<String, Object>> eventReads() {
        return businessReads().stream().filter(row -> ((String) row.get("query"))
                .matches("(?is).*from\\s+calendar[.]event.*")).toList();
    }

    List<Map<String, Object>> businessReads() {
        return jdbc.sql("""
                SELECT query, calls, rows FROM pg_stat_statements
                WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
                  AND query ~* '^(select|with)' AND query ~* 'calendar[.](account|source|event|google_credential)'
                  AND query NOT ILIKE '%pg_stat_statements%'
                ORDER BY query
                """).query((row, index) -> Map.<String, Object>of("query", row.getString("query"),
                        "calls", row.getLong("calls"), "rows", row.getLong("rows"))).list();
    }

    long returnedEventRows() {
        return eventReads().stream().mapToLong(row -> (long) row.get("rows")).sum();
    }

    long fullEventJsonBytes(UUID userId) {
        return jdbc.sql("SELECT sum(octet_length(to_jsonb(e)::text)) FROM calendar.event e WHERE user_id = ?")
                .param(userId).query(Long.class).single();
    }
}
