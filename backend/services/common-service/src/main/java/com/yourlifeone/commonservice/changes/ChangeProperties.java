package com.yourlifeone.commonservice.changes;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where change events go ({@code app.changes.*}).
 *
 * @param exchange durable topic exchange on the server's RabbitMQ; the routing key is the event type
 */
@ConfigurationProperties("app.changes")
public record ChangeProperties(String exchange) {
    public static final String DEFAULT_EXCHANGE = "yourlife.changes";

    public ChangeProperties {
        exchange = exchange == null || exchange.isBlank() ? DEFAULT_EXCHANGE : exchange;
    }
}
