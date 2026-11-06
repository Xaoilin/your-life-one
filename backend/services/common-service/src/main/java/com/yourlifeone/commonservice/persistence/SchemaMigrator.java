package com.yourlifeone.commonservice.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Applies a service's Flyway migrations to its own PostgreSQL schema. Migration starts in the
 * background once the service is ready and is retried on first data access, so an unreachable
 * database never delays startup or health checks.
 */
public final class SchemaMigrator implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrator.class);

    private final Flyway flyway;
    private final String schema;
    private volatile boolean migrated;

    /** Migrations are read from {@code classpath:db/migration/<schema>}. */
    public SchemaMigrator(DataSource dataSource, String schema) {
        this.schema = schema;
        this.flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .createSchemas(true)
                .locations("classpath:db/migration/" + schema)
                .load();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread.ofVirtual().name("schema-migrator-" + schema).start(() -> {
            try {
                ensureMigrated();
                logger.info("Schema {} is up to date", schema);
            } catch (DatabaseUnavailableException exception) {
                logger.warn("Schema {} migration deferred to first use: {}", schema,
                        exception.getCause() == null ? "unknown" : exception.getCause().getMessage());
            }
        });
    }

    /** Idempotent and thread-safe; throws {@link DatabaseUnavailableException} when it cannot complete. */
    public void ensureMigrated() {
        if (migrated) return;
        synchronized (this) {
            if (migrated) return;
            try {
                flyway.migrate();
                migrated = true;
            } catch (FlywayException exception) {
                throw new DatabaseUnavailableException("Schema " + schema + " migration failed", exception);
            }
        }
    }
}
