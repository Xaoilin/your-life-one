package com.yourlifeone.finance.infrastructure.persistence;

import com.yourlifeone.finance.testing.FinanceSamples;
import com.yourlifeone.finance.testing.FinanceTest;
import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time imports of the React app's finance and equity records ({@code public.yourlifeone_records}), with fake
 * data, each run against its own schema so they cannot disturb the other tests. They use their own
 * connections: the service's two-connection pool may be busy migrating.
 */
@FinanceTest
class ReactAppImportTest {
    private static final UUID USER = UUID.randomUUID();

    @Autowired
    private DataSource dataSource;

    private DriverManagerDataSource direct() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        return new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    private JdbcClient jdbc() {
        return JdbcClient.create(direct());
    }

    private void migrate(String domain, String schema) {
        Flyway.configure().dataSource(direct()).schemas(schema).createSchemas(true)
                .locations("classpath:db/migration/" + domain).load().migrate();
    }

    private void insert(JdbcClient jdbc, String collection, String id, String payload, boolean deleted) {
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, position bigint, deleted_at timestamptz, created_at timestamptz DEFAULT now(),
                    updated_at timestamptz DEFAULT now())""").update();
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload, deleted_at)"
                        + " VALUES (?, ?, ?, ?::jsonb, " + (deleted ? "now()" : "NULL") + ")")
                .params(USER, collection, id, payload).update();
    }

    private static String one(JdbcClient jdbc, String sql) {
        return jdbc.sql(sql).param(USER).query(String.class).single();
    }

    @Test
    void importsTheLedgerAndTheReviewWithEveryField() {
        var jdbc = jdbc();
        insert(jdbc, "financeAccounts", "acc-current", """
                {"id":"acc-current","name":"Example Bank","type":"current","balance":12345,"currency":"GBP",
                 "color":"#22c55e","icon":"bank","includeInNetWorth":true,"sortOrder":1,
                 "createdAt":"2026-01-02T03:04:05.678Z","updatedAt":"2026-09-01T10:00:00.000Z"}""", false);
        insert(jdbc, "financeAccounts", "acc-savings", """
                {"id":"acc-savings","name":"Example Savings","type":"savings","balance":-500,"currency":"EUR",
                 "color":"#0ea5e9","icon":"piggy","includeInNetWorth":false,"sortOrder":0,
                 "createdAt":"2026-01-03T00:00:00Z","updatedAt":"2026-01-03T00:00:00Z"}""", false);
        insert(jdbc, "financeAccounts", "acc-old", "{\"id\":\"acc-old\",\"name\":\"Deleted\",\"balance\":1}", true);
        insert(jdbc, "transactions", "tx-transfer", """
                {"id":"tx-transfer","type":"transfer","amount":2500,"category":"transfer","accountId":"acc-current",
                 "toAccountId":"acc-savings","description":"Example move","date":"2026-09-10","tags":["example","move"],
                 "createdAt":"2026-09-10T08:00:00Z","updatedAt":"2026-09-10T09:00:00Z"}""", false);
        insert(jdbc, "transactions", "tx-orphan-transfer", """
                {"id":"tx-orphan-transfer","type":"transfer","amount":100,"category":"transfer","accountId":"acc-current",
                 "toAccountId":"acc-old","description":"","date":"2026-09-11"}""", false);
        insert(jdbc, "transactions", "tx-income", """
                {"id":"tx-income","type":"income","amount":99.6,"category":"salary","accountId":"acc-savings",
                 "description":"Example pay","date":"2026-09-12"}""", false);
        insert(jdbc, "transactions", "tx-zero", """
                {"id":"tx-zero","type":"expense","amount":0,"category":"groceries","accountId":"acc-current",
                 "description":"Example zero","date":"2026-09-13"}""", false);
        insert(jdbc, "transactions", "tx-no-account", """
                {"id":"tx-no-account","type":"expense","amount":100,"category":"groceries","accountId":"acc-old",
                 "description":"","date":"2026-09-12"}""", false);
        insert(jdbc, "transactions", "tx-deleted", """
                {"id":"tx-deleted","type":"expense","amount":100,"category":"groceries","accountId":"acc-current",
                 "description":"","date":"2026-09-12"}""", true);
        insert(jdbc, "financeBudgets", "budget-1", """
                {"id":"budget-1","category":"groceries","monthlyLimit":30000,
                 "createdAt":"2026-02-01T00:00:00Z","updatedAt":"2026-02-02T00:00:00Z"}""", false);
        insert(jdbc, "savingsGoals", "goal-1", """
                {"id":"goal-1","name":"Example holiday","targetAmount":100000,"currentAmount":2500,
                 "linkedAccountId":"acc-savings","icon":"plane","deadline":"2027-06-01","completed":true,
                 "completedAt":"2026-08-01T12:00:00Z","createdAt":"2026-03-01T00:00:00Z","updatedAt":"2026-08-01T12:00:00Z"}""",
                false);
        insert(jdbc, "savingsGoals", "goal-2", """
                {"id":"goal-2","name":"Example fund","targetAmount":500,"currentAmount":0,"linkedAccountId":"acc-old",
                 "icon":"star","completed":false}""", false);
        var review = FinanceSamples.reviewDraft();
        review.put("id", "current").put("createdAt", "2026-09-21T18:06:59.123456+00:00")
                .put("updatedAt", "2026-09-22T07:00:00.654321+00:00");
        insert(jdbc, "financeReviews", "current", review.toString(), false);

        migrate("finance", "finance_import_test");

        assertEquals("Example Bank|current|12345|GBP|#22c55e|bank|true|1|true",
                one(jdbc, "SELECT name || '|' || type || '|' || balance || '|' || currency || '|' || color || '|' || icon"
                        + " || '|' || include_in_net_worth || '|' || sort_order || '|' || (created_at = '2026-01-02T03:04:05.678Z')"
                        + " FROM finance_import_test.account WHERE user_id = ? AND id = 'acc-current'"));
        assertEquals("savings|-500|EUR|false|0", one(jdbc, "SELECT type || '|' || balance || '|' || currency || '|'"
                + " || include_in_net_worth || '|' || sort_order FROM finance_import_test.account"
                + " WHERE user_id = ? AND id = 'acc-savings'"));
        assertEquals("2", one(jdbc, "SELECT count(*)::text FROM finance_import_test.account WHERE user_id = ?"));

        assertEquals("transfer|2500|transfer|acc-current|acc-savings|Example move|2026-09-10|{example,move}|true",
                one(jdbc, "SELECT type || '|' || amount || '|' || category || '|' || account_id || '|' || to_account_id"
                        + " || '|' || description || '|' || date || '|' || tags::text || '|' || (updated_at = '2026-09-10T09:00:00Z')"
                        + " FROM finance_import_test.transaction WHERE user_id = ? AND id = 'tx-transfer'"));
        assertEquals("expense|transfer|", one(jdbc, "SELECT type || '|' || category || '|' || coalesce(to_account_id, '')"
                + " FROM finance_import_test.transaction WHERE user_id = ? AND id = 'tx-orphan-transfer'"));
        assertEquals("income|100|salary|{}", one(jdbc, "SELECT type || '|' || amount || '|' || category || '|' || tags::text"
                + " FROM finance_import_test.transaction WHERE user_id = ? AND id = 'tx-income'"));
        assertEquals("expense|0", one(jdbc, "SELECT type || '|' || amount"
                + " FROM finance_import_test.transaction WHERE user_id = ? AND id = 'tx-zero'"));
        assertEquals("4", one(jdbc, "SELECT count(*)::text FROM finance_import_test.transaction WHERE user_id = ?"));

        assertEquals("groceries|30000|true", one(jdbc, "SELECT category || '|' || monthly_limit || '|'"
                + " || (created_at = '2026-02-01T00:00:00Z') FROM finance_import_test.budget WHERE user_id = ?"));
        assertEquals("Example holiday|100000|2500|acc-savings|plane|2027-06-01|true|true",
                one(jdbc, "SELECT name || '|' || target_amount || '|' || current_amount || '|' || linked_account_id"
                        + " || '|' || icon || '|' || deadline || '|' || completed || '|' || (completed_at = '2026-08-01T12:00:00Z')"
                        + " FROM finance_import_test.savings_goal WHERE user_id = ? AND id = 'goal-1'"));
        assertEquals("|false|", one(jdbc, "SELECT coalesce(linked_account_id, '') || '|' || completed || '|'"
                + " || coalesce(deadline::text, '') FROM finance_import_test.savings_goal WHERE user_id = ? AND id = 'goal-2'"));

        assertEquals("2026-09-20|true|true|false|2|Example Lender",
                one(jdbc, "SELECT as_of || '|' || (created_at = '2026-09-21T18:06:59.123456Z') || '|'"
                        + " || (updated_at = '2026-09-22T07:00:00.654321Z') || '|' || (document -> 'id' IS NOT NULL) || '|'"
                        + " || jsonb_array_length(document -> 'loans') || '|' || (document -> 'loans' -> 0 ->> 'lender')"
                        + " FROM finance_import_test.review WHERE user_id = ?"));
    }

    @Test
    void importsEquityPositionsWithTheirDocument() {
        var jdbc = jdbc();
        var position = FinanceSamples.positionDraft("Example Co");
        position.put("id", "pos-1").put("createdAt", "2026-09-21T17:29:21.5+00:00")
                .put("updatedAt", "2026-09-23T10:00:00+00:00");
        insert(jdbc, "equityPositions", "pos-1", position.toString(), false);
        insert(jdbc, "equityPositions", "pos-gone", FinanceSamples.positionDraft("Removed Co").toString(), true);

        migrate("equity", "equity_import_test");

        assertEquals("pos-1|Example Co|2026-09-20|true|true|false|100|2",
                one(jdbc, "SELECT id || '|' || company || '|' || as_of || '|' || (created_at = '2026-09-21T17:29:21.5Z') || '|'"
                        + " || (updated_at = '2026-09-23T10:00:00Z') || '|'"
                        + " || (document -> 'updatedAt' IS NOT NULL) || '|' || (document ->> 'ownedShares') || '|'"
                        + " || jsonb_array_length(document -> 'grants') FROM equity_import_test.position WHERE user_id = ?"));
    }
}
