package com.yourlifeone.prayer.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Content-free counters from the isolated test PostgreSQL; never resets hosted statistics. */
final class PrayerPassEvidence {
    private final JdbcClient jdbc;

    PrayerPassEvidence(JdbcClient jdbc) {
        this.jdbc = jdbc;
        jdbc.sql("CREATE EXTENSION IF NOT EXISTS pg_stat_statements").update();
    }

    List<Map<String, Object>> reads() {
        return jdbc.sql("""
                SELECT query, calls, rows FROM pg_stat_statements
                WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
                  AND query ~* '^(select|with)' AND query ~* 'prayer[.]'
                  AND query NOT ILIKE '%pg_stat_statements%' AND query NOT ILIKE '%count(%'
                ORDER BY query
                """).query((row, index) -> Map.<String, Object>of("query", row.getString("query"),
                        "calls", row.getLong("calls"), "rows", row.getLong("rows"))).list();
    }

    List<Map<String, Object>> delta(List<Map<String, Object>> before) {
        return reads().stream().map(after -> {
            var previous = before.stream().filter(row -> row.get("query").equals(after.get("query")))
                    .findFirst().orElse(Map.of("calls", 0L, "rows", 0L));
            return Map.<String, Object>of("query", after.get("query"),
                    "calls", (long) after.get("calls") - (long) previous.get("calls"),
                    "rows", (long) after.get("rows") - (long) previous.get("rows"));
        }).filter(row -> (long) row.get("calls") > 0).toList();
    }

    static long calls(List<Map<String, Object>> rows, String pattern) {
        return rows.stream().filter(row -> ((String) row.get("query")).matches("(?is).*" + pattern + ".*"))
                .mapToLong(row -> (long) row.get("calls")).sum();
    }

    List<Map<String, Object>> indexes() {
        return jdbc.sql("""
                SELECT tablename, indexname, indexdef FROM pg_indexes WHERE schemaname = 'prayer'
                  AND tablename IN ('user_day', 'schedule_day', 'outcome') ORDER BY tablename, indexname
                """).query((row, index) -> Map.<String, Object>of("table", row.getString("tablename"),
                        "name", row.getString("indexname"), "definition", row.getString("indexdef"))).list();
    }

    List<Map<String, Object>> plans(UUID user, List<Map<String, Object>> reads) {
        return reads.stream().map(row -> (String) row.get("query"))
                .filter(query -> query.matches("(?is).*from\\s+prayer[.](user_day|outcome).*"))
                .map(query -> {
                    var bound = query.replace("$3", "DATE '2026-09-25'")
                            .replace("$2", query.toLowerCase().contains("order by") ? "1" : "DATE '2026-09-24'")
                            .replace("$1", "UUID '" + user + "'");
                    var plan = jdbc.sql("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + bound).query(String.class).single();
                    return Map.<String, Object>of("query", query, "plan", plan);
                }).toList();
    }
}
