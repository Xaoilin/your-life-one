package com.yourlifeone.finance.infrastructure.database;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Reads the user's AI-agent approvals that the React app records in Supabase (one table per domain).
 * Native SQL because the table name comes from validated configuration and lives outside this
 * service's schemas.
 *
 * <p>Approvals stay in Supabase, where the React app grants and revokes them.
 */
public class AgentApprovalRepository {
    private final JdbcClient jdbc;

    public AgentApprovalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** False when the table does not exist, so a missing table never grants access. */
    public boolean isApproved(String table, UUID userId, String clientId) {
        var exists = jdbc.sql("SELECT to_regclass(:table) IS NOT NULL").param("table", table)
                .query(Boolean.class).single();
        if (!exists) return false;
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + table
                        + " WHERE user_id = :userId AND client_id = :clientId AND revoked_at IS NULL)")
                .param("userId", userId)
                .param("clientId", clientId)
                .query(Boolean.class).single();
    }
}
