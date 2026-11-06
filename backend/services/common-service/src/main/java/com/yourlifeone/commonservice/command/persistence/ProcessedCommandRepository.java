package com.yourlifeone.commonservice.command.persistence;

import com.yourlifeone.commonservice.command.WriteCommand;
import com.yourlifeone.commonservice.command.WriteCommandProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The processed-command table (the queue's inbox). Native SQL because claiming a message is an
 * {@code INSERT ... ON CONFLICT DO NOTHING}; the table name comes from validated configuration.
 */
public class ProcessedCommandRepository {
    private final JdbcClient jdbc;
    private final String table;

    public ProcessedCommandRepository(JdbcClient jdbc, WriteCommandProperties properties) {
        this.jdbc = jdbc;
        this.table = properties.processedTable();
    }

    public Optional<ProcessedCommand> find(UUID messageId) {
        return jdbc.sql("SELECT message_id, user_id, command_type, fingerprint, response_status, response_body"
                        + " FROM " + table + " WHERE message_id = :messageId AND response_status IS NOT NULL")
                .param("messageId", messageId)
                .query((row, index) -> ProcessedCommand.builder()
                        .messageId(row.getObject("message_id", UUID.class))
                        .userId(row.getObject("user_id", UUID.class))
                        .commandType(row.getString("command_type"))
                        .fingerprint(row.getString("fingerprint"))
                        .responseStatus(row.getInt("response_status"))
                        .responseBody(row.getString("response_body"))
                        .build())
                .optional();
    }

    /**
     * Claims the command for this transaction. A concurrent duplicate waits here until the first
     * commits, then finds it processed; false means the command was already processed.
     */
    public boolean claim(WriteCommand command) {
        return jdbc.sql("INSERT INTO " + table + " (message_id, user_id, command_type, fingerprint)"
                        + " VALUES (:messageId, :userId, :type, :fingerprint) ON CONFLICT (message_id) DO NOTHING")
                .param("messageId", command.messageId())
                .param("userId", command.userId())
                .param("type", command.type())
                .param("fingerprint", command.fingerprint())
                .update() == 1;
    }

    public void complete(UUID messageId, int responseStatus, String responseBody) {
        jdbc.sql("UPDATE " + table + " SET response_status = :status, response_body = :body"
                        + " WHERE message_id = :messageId")
                .param("status", responseStatus)
                .param("body", responseBody)
                .param("messageId", messageId)
                .update();
    }

    /** Forgets processed writes older than the retention, measured on the database clock. */
    public int deleteOlderThan(Duration retention) {
        return jdbc.sql("DELETE FROM " + table + " WHERE processed_at < now() - make_interval(secs => :seconds)")
                .param("seconds", retention.toSeconds())
                .update();
    }
}
