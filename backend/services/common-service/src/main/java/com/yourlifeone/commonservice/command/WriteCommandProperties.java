package com.yourlifeone.commonservice.command;

import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How a service routes its writes through RabbitMQ ({@code app.write-commands.*}).
 *
 * @param queue durable queue this service's write commands go through, e.g. {@code yourlife.prayer.commands}
 * @param processedTable schema-qualified table recording processed commands, e.g. {@code prayer.processed_command}
 * @param replyTimeout how long an API request waits for its write to be processed
 * @param consumers how many writes this service processes at once; keeps the database pool free for reads
 * @param retention how long a processed write is remembered; a retry after this is applied as a new write
 */
@ConfigurationProperties("app.write-commands")
public record WriteCommandProperties(
        String queue, String processedTable, Duration replyTimeout, Integer consumers, Duration retention) {
    private static final Pattern TABLE = Pattern.compile("[a-z_]+\\.[a-z_]+");

    public WriteCommandProperties {
        if (queue == null || queue.isBlank()) {
            throw new IllegalArgumentException("app.write-commands.queue is required");
        }
        if (processedTable == null || !TABLE.matcher(processedTable).matches()) {
            throw new IllegalArgumentException("app.write-commands.processed-table must be schema.table");
        }
        replyTimeout = replyTimeout == null ? Duration.ofSeconds(8) : replyTimeout;
        consumers = consumers == null ? 1 : consumers;
        if (consumers < 1) {
            throw new IllegalArgumentException("app.write-commands.consumers must be at least 1");
        }
        retention = retention == null ? Duration.ofDays(30) : retention;
        if (retention.compareTo(replyTimeout) <= 0) {
            throw new IllegalArgumentException("app.write-commands.retention must be longer than the reply timeout");
        }
    }
}
