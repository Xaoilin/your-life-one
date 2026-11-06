package com.yourlifeone.commonservice.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real PostgreSQL for integration tests. Import it into a {@code @SpringBootTest}; Spring's
 * cached test context shares one container across every test class with the same configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements")
                .withInitScript("db/test-auth.sql");
    }
}
