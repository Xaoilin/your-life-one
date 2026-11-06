package com.yourlifeone.life.config;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which domains AI agents may use ({@code app.life.agent-access.*}). An agent's Supabase OAuth token
 * carries a {@code client_id}; it may call {@code /api/life/v1/<domain>} only while the user's approval
 * of that client is active in the domain's approval table. Domains without a table refuse agents.
 *
 * @param approvals domain (first path segment after {@code /api/life/v1/}) to schema-qualified approval table
 */
@ConfigurationProperties("app.life.agent-access")
public record AgentAccessProperties(Map<String, String> approvals) {
    private static final Pattern TABLE = Pattern.compile("[a-z_]+\\.[a-z_]+");

    public AgentAccessProperties {
        approvals = approvals == null ? Map.of() : Map.copyOf(approvals);
        approvals.values().forEach(table -> {
            if (!TABLE.matcher(table).matches()) {
                throw new IllegalArgumentException("app.life.agent-access.approvals tables must be schema.table");
            }
        });
    }
}
