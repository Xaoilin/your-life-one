package com.yourlifeone.knowledge.config;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which endpoints AI agents may use ({@code app.knowledge.agent-access.*}). An agent's Supabase OAuth
 * token carries a {@code client_id}; it may call {@code /api/knowledge/v1/<path>} only while the user's
 * approval of that client is active in the path's approval table. Paths without a table refuse agents.
 *
 * @param approvals path after {@code /api/knowledge/v1/} to schema-qualified approval table
 */
@ConfigurationProperties("app.knowledge.agent-access")
public record AgentAccessProperties(Map<String, String> approvals) {
    private static final Pattern TABLE = Pattern.compile("[a-z_]+\\.[a-z_]+");

    public AgentAccessProperties {
        approvals = approvals == null ? Map.of() : Map.copyOf(approvals);
        approvals.values().forEach(table -> {
            if (!TABLE.matcher(table).matches()) {
                throw new IllegalArgumentException("app.knowledge.agent-access.approvals tables must be schema.table");
            }
        });
    }
}
