package com.yourlifeone.commonservice.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * A real RabbitMQ for integration tests. Import it next to {@link PostgresTestContainer}; the cached
 * test context shares one broker across every test class with the same configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitTestContainer {
    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return new RabbitMQContainer("rabbitmq:4-alpine");
    }
}
