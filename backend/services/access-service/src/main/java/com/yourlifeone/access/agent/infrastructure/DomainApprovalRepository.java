package com.yourlifeone.access.agent.infrastructure;

import com.yourlifeone.access.agent.api.response.ClientApproval;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Fixed legacy table names preserve the guards already enforced by the product services. */
@Repository
public class DomainApprovalRepository {
    private final JdbcClient database;

    public DomainApprovalRepository(JdbcClient database) {
        this.database = database;
    }

    public List<ClientApproval> list(McpDomain domain, UUID user) {
        return database.sql("select client_id,client_name,approved_at,revoked_at from " + table(domain)
                        + " where user_id=:user order by approved_at desc,client_id")
                .param("user", user).query(this::map).list();
    }

    public ClientApproval approve(McpDomain domain, UUID user, String clientId, String name) {
        return database.sql("insert into " + table(domain) + " (user_id,client_id,client_name) values (:user,:client,:name) "
                        + "on conflict (user_id,client_id) do update set client_name=excluded.client_name,"
                        + "approved_at=now(),revoked_at=null,updated_at=now() returning client_id,client_name,approved_at,revoked_at")
                .param("user", user).param("client", clientId).param("name", name).query(this::map).single();
    }

    public ClientApproval revoke(McpDomain domain, UUID user, String clientId) {
        return database.sql("update " + table(domain) + " set revoked_at=coalesce(revoked_at,now()),updated_at=now() "
                        + "where user_id=:user and client_id=:client returning client_id,client_name,approved_at,revoked_at")
                .param("user", user).param("client", clientId).query(this::map).optional()
                .orElseThrow(() -> new IllegalArgumentException("This client has no approval in this domain."));
    }

    public void revokeGrant(UUID user, String registeredClientId) {
        database.sql("delete from identity.oauth2_authorization where principal_name=:user and registered_client_id=:client")
                .param("user", user.toString()).param("client", registeredClientId).update();
        database.sql("delete from identity.oauth2_authorization_consent where principal_name=:user and registered_client_id=:client")
                .param("user", user.toString()).param("client", registeredClientId).update();
    }

    private String table(McpDomain domain) {
        return switch (domain) {
            case INVENTORY -> "public.yourlifeone_inventory_oauth_clients";
            case EMPLOYMENT -> "public.yourlifeone_employment_oauth_clients";
            case EQUITY -> "public.yourlifeone_equity_oauth_clients";
            case FINANCE -> "public.yourlifeone_finance_oauth_clients";
        };
    }

    private ClientApproval map(ResultSet row, int index) throws SQLException {
        var revoked = row.getTimestamp("revoked_at");
        return ClientApproval.builder().clientId(row.getString("client_id")).clientName(row.getString("client_name"))
                .approvedAt(row.getTimestamp("approved_at").toInstant()).revokedAt(revoked == null ? null : revoked.toInstant()).build();
    }
}
