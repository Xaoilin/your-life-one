package com.yourlifeone.finance.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The finance numbers for the Grafana business dashboard (the "Finance" row). Counts only: amounts,
 * names and IDs never become metrics or tags.
 */
@Configuration(proxyBeanMethods = false)
public class FinanceMetricsConfiguration {
    private static final String TOTAL = "total";

    /** Accounts kept, by type. */
    @Bean
    BusinessGauge financeAccountsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.accounts", "Manual accounts by type",
                () -> jdbc.sql("SELECT type, count(*) AS total FROM finance.account GROUP BY type")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL), "type", rs.getString("type")))
                        .list());
    }

    /** Transactions recorded, by type (income, expense, transfer). */
    @Bean
    BusinessGauge financeTransactionsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.transactions", "Transactions by type",
                () -> jdbc.sql("SELECT type, count(*) AS total FROM finance.transaction GROUP BY type")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL), "type", rs.getString("type")))
                        .list());
    }

    @Bean
    BusinessGauge financeBudgetsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.budgets", "Monthly budgets",
                () -> jdbc.sql("SELECT count(*) AS total FROM finance.budget")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL)))
                        .list());
    }

    /** Savings goals, by whether they are completed. */
    @Bean
    BusinessGauge financeSavingsGoalsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.savings.goals", "Savings goals by whether they are completed",
                () -> jdbc.sql("SELECT completed, count(*) AS total FROM finance.savings_goal GROUP BY completed")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL),
                                "completed", String.valueOf(rs.getBoolean("completed"))))
                        .list());
    }

    @Bean
    BusinessGauge financeEquityPositionsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.equity.positions", "Equity positions",
                () -> jdbc.sql("SELECT count(*) AS total FROM equity.position")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL)))
                        .list());
    }

    /** Months of cash flow in the banking reviews. */
    @Bean
    BusinessGauge financeReviewMonthsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.review.months", "Months of cash flow in banking reviews",
                () -> jdbc.sql("""
                        SELECT coalesce(sum(CASE WHEN jsonb_typeof(document -> 'months') = 'array'
                                                 THEN jsonb_array_length(document -> 'months') END), 0) AS total
                        FROM finance.review""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL)))
                        .list());
    }

    /** Loans marked active in the banking reviews. */
    @Bean
    BusinessGauge financeActiveLoansGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.finance.review.loans.active", "Active loans in banking reviews",
                () -> jdbc.sql("""
                        SELECT count(*) AS total
                        FROM finance.review r
                        CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(r.document -> 'loans') = 'array'
                                                                     THEN r.document -> 'loans' ELSE '[]' END) loan
                        WHERE loan ->> 'status' = 'active'""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(TOTAL)))
                        .list());
    }
}
