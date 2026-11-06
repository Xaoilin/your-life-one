package com.yourlifeone.finance.testing;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The approval tables the React app keeps in Supabase, created in the test database like there. */
public final class AgentApprovals {
    public static final String FINANCE = "yourlifeone_finance_oauth_clients";
    public static final String EQUITY = "yourlifeone_equity_oauth_clients";

    private final JdbcClient jdbc;

    public AgentApprovals(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
        for (var table : new String[] {FINANCE, EQUITY}) {
            jdbc.sql("CREATE TABLE IF NOT EXISTS public." + table + " (user_id uuid, client_id text, revoked_at timestamptz)")
                    .update();
        }
    }

    public void approve(String table, UUID user, String client, boolean revoked) {
        jdbc.sql("INSERT INTO public." + table + " (user_id, client_id, revoked_at) VALUES (?, ?, "
                        + (revoked ? "now()" : "NULL") + ")")
                .params(user, client).update();
    }
}
