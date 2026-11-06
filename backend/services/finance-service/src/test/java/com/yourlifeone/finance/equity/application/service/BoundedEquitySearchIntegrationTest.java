package com.yourlifeone.finance.equity.application.service;

import com.yourlifeone.commonservice.testing.PostgresQueryCounts;
import com.yourlifeone.commonservice.testing.PostgresPlanEvidence;
import com.yourlifeone.finance.equity.infrastructure.persistence.repository.PositionRepository;
import java.util.Arrays;
import java.util.Map;
import org.springframework.data.jpa.repository.Query;
import com.yourlifeone.finance.infrastructure.persistence.FinanceSchemas;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@FinanceTest
class BoundedEquitySearchIntegrationTest {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private FinanceSchemas schemas;
    @Autowired
    private EquityService equity;

    private JdbcClient jdbc;
    private PostgresQueryCounts counts;

    @BeforeEach
    void setUp() {
        schemas.ensureMigrated();
        jdbc = JdbcClient.create(dataSource);
        counts = new PostgresQueryCounts(dataSource);
    }

    @Test
    void limitOneKeepsTotalsOrderOwnershipLiteralWildcardsAndWholeDocumentsAsPositionsGrow() {
        var user = UUID.randomUUID();
        seed(user, 1, 40);
        seed(UUID.randomUUID(), 1, 40);
        assertBound(user, 40);
        seed(user, 41, 340);
        assertBound(user, 340);
        assertThat(equity.search(user, "100_", 1, 0).total()).isZero();
        assertThat(equity.search(user, "100%", 1, 340).positions()).isEmpty();
        assertThat(equity.search(user, "100%", 1, 340).total()).isEqualTo(340);
        assertThat(equity.search(UUID.randomUUID(), "", 1, 0).total()).isZero();
    }

    private void assertBound(UUID user, int total) {
        counts.reset();
        var result = equity.search(user, " EXAMPLE 100% ", 1, 1);
        assertThat(result.total()).isEqualTo(total);
        assertThat(result.positions()).extracting("id").containsExactly("position0002");
        assertThat(result.positions().getFirst().grants()).hasSize(2);
        var queries = counts.selects();
        var calls = queries.values().stream().mapToLong(Long::longValue).sum();
        var rows = jdbc.sql("SELECT coalesce(sum(rows),0) FROM pg_stat_statements "
                        + "WHERE query ~* '^select' AND query NOT ILIKE '%pg_stat_statements%'")
                .query(Long.class).single();
        System.out.println("KAN-328 equity total=" + total + " selects=" + calls + " returned_rows=" + rows);
        queries.forEach((query, count) -> System.out.println("KAN-328 SQL calls=" + count + " " + query));
        assertThat(calls).isLessThanOrEqualTo(2);
        assertThat(rows).isLessThanOrEqualTo(2);
        if (Boolean.getBoolean("kan328.plans")) {
            jdbc.sql("ANALYZE equity.position").update();
            PostgresPlanEvidence.catalogue(jdbc);
            var query = Arrays.stream(PositionRepository.class.getMethods())
                    .filter(method -> method.getName().equals("search"))
                    .findFirst().orElseThrow().getAnnotation(Query.class).value();
            PostgresPlanEvidence.capture(jdbc, "equity", query,
                    Map.of("userId", user, "company", "EXAMPLE 100%", "limit", 1, "offset", 1));
            PostgresPlanEvidence.capture(jdbc, "equity-old-parents", "SELECT * FROM equity.position "
                    + "WHERE user_id=:userId AND upper(company) LIKE '%EXAMPLE 100\\%%' ESCAPE '\\' ORDER BY id",
                    Map.of("userId", user));
        }
    }

    private void seed(UUID user, int first, int last) {
        jdbc.sql("""
                INSERT INTO equity.position (user_id,id,document,company,as_of,created_at,updated_at)
                SELECT ?, 'position'||lpad(n::text,4,'0'),cast(? AS jsonb),'Example 100%',DATE '2026-09-20',now(),now()
                FROM generate_series(?,?) n
                """).params(user, FinanceSamples.positionDraft("Example 100%").toString(), first, last).update();
    }
}
