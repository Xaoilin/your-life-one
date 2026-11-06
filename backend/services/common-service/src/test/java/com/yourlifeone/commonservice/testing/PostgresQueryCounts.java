package com.yourlifeone.commonservice.testing;

import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL's own executed SELECT counters, including JDBC, JPA and interceptor queries. */
public final class PostgresQueryCounts {
    private final JdbcClient jdbc;

    public PostgresQueryCounts(DataSource dataSource) {
        jdbc = JdbcClient.create(dataSource);
        jdbc.sql("CREATE EXTENSION IF NOT EXISTS pg_stat_statements").update();
    }

    public void reset() {
        jdbc.sql("SELECT pg_stat_statements_reset()").query(String.class).single();
    }

    public Map<String, Long> selects() {
        return jdbc.sql("SELECT query, calls FROM pg_stat_statements WHERE dbid = "
                        + "(SELECT oid FROM pg_database WHERE datname = current_database()) "
                        + "AND query ~* '^(select|with|table)' AND query NOT ILIKE '%pg_stat_statements%'")
                .query((row, index) -> Map.entry(row.getString("query"), row.getLong("calls")))
                .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
