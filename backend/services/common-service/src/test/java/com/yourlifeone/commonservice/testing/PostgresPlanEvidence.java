package com.yourlifeone.commonservice.testing;

import java.util.Map;
import java.sql.Types;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Opt-in synthetic query plans; never enabled against an external database by the test suite. */
public final class PostgresPlanEvidence {
    private PostgresPlanEvidence() {
    }

    public static void capture(JdbcClient jdbc, String label, String sql, Map<String, ?> parameters) {
        if (!Boolean.getBoolean("kan328.plans")) return;
        var statement = jdbc.sql("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql);
        parameters.forEach((name, value) -> {
            if (value == null) statement.param(name, null, Types.VARCHAR);
            else statement.param(name, value);
        });
        var plan = statement.query(String.class).single();
        System.out.println("KAN-328 QUERY " + label + "\n" + sql + "\nKAN-328 END QUERY");
        System.out.println("KAN-328 PLAN " + label + " " + plan.replace("\n", ""));
    }

    public static void catalogue(JdbcClient jdbc) {
        if (!Boolean.getBoolean("kan328.plans")) return;
        System.out.println("KAN-328 PostgreSQL " + jdbc.sql("SELECT version()").query(String.class).single());
        jdbc.sql("SELECT indexdef FROM pg_indexes WHERE schemaname IN ('inventory','jobs','equity') "
                        + "AND tablename NOT IN ('flyway_schema_history','processed_command') ORDER BY indexname")
                .query(String.class).list().forEach(index -> System.out.println("KAN-328 INDEX " + index));
    }
}
