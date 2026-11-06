package com.yourlifeone.finance.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.finance.infrastructure.persistence.FinanceSchemas;
import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The finance numbers, read from a real PostgreSQL and published for Grafana. */
@FinanceTest
class FinanceMetricsTest {
    @Autowired
    private FinanceSchemas schemas;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BusinessGaugePublisher gauges;

    @Autowired
    private MeterRegistry meters;

    private final UUID user = UUID.randomUUID();

    private double gauge(String name, String... tags) {
        return meters.get(name).tags(tags).gauge().value();
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    @Test
    void publishesLedgerReviewAndEquityNumbers() {
        schemas.ensureMigrated();
        jdbc.sql("""
                INSERT INTO finance.account (user_id, id, name, type, balance, currency, color, icon, include_in_net_worth,
                                             sort_order, created_at, updated_at)
                VALUES (?, 'a', 'Example Bank', 'isa', 12345, 'GBP', '', '', true, 0, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO finance.transaction (user_id, id, type, amount, category, account_id, description, date,
                                                 created_at, updated_at)
                VALUES (?, 't', 'income', 100, 'salary', 'a', '', current_date, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO finance.budget (user_id, id, category, monthly_limit, created_at, updated_at)
                VALUES (?, 'b', 'groceries', 100, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO finance.savings_goal (user_id, id, name, target_amount, current_amount, icon, completed,
                                                  completed_at, created_at, updated_at)
                VALUES (?, 'g', 'Example goal', 100, 100, '', true, now(), now(), now())""").param(user).update();
        jdbc.sql("INSERT INTO finance.review (user_id, document, as_of, created_at, updated_at)"
                        + " VALUES (?, ?::jsonb, current_date, now(), now())")
                .params(user, FinanceSamples.reviewDraft().toString()).update();
        jdbc.sql("INSERT INTO equity.position (user_id, id, document, company, as_of, created_at, updated_at)"
                        + " VALUES (?, 'p', ?::jsonb, 'Example Co', current_date, now(), now())")
                .params(user, FinanceSamples.positionDraft("Example Co").toString()).update();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(count("SELECT count(*) FROM finance.account WHERE type = 'isa'"),
                gauge("yourlife.finance.accounts", "type", "isa"));
        assertEquals(count("SELECT count(*) FROM finance.transaction WHERE type = 'income'"),
                gauge("yourlife.finance.transactions", "type", "income"));
        assertEquals(count("SELECT count(*) FROM finance.budget"), gauge("yourlife.finance.budgets"));
        assertEquals(count("SELECT count(*) FROM finance.savings_goal WHERE completed"),
                gauge("yourlife.finance.savings.goals", "completed", "true"));
        assertEquals(count("SELECT count(*) FROM equity.position"), gauge("yourlife.finance.equity.positions"));
        assertEquals(count("SELECT coalesce(sum(jsonb_array_length(document -> 'months')), 0) FROM finance.review"),
                gauge("yourlife.finance.review.months"));
        assertEquals(count("SELECT count(*) FROM finance.review r, jsonb_array_elements(r.document -> 'loans') loan"
                + " WHERE loan ->> 'status' = 'active'"), gauge("yourlife.finance.review.loans.active"));
    }
}
