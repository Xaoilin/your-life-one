package com.yourlifeone.knowledge.infrastructure.persistence;

import com.yourlifeone.commonservice.persistence.DatabaseUnavailableException;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Migrates the service's schemas one after another: in the background once the service is ready, and
 * again before any data request until that has succeeded. Migrating them together would let each
 * Flyway run hold connections the others wait for, and the pool has only two.
 */
public class KnowledgeSchemas implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSchemas.class);

    private final List<SchemaMigrator> migrators;
    private volatile boolean migrated;

    /** @param migrators in the order they run */
    public KnowledgeSchemas(List<SchemaMigrator> migrators) {
        this.migrators = List.copyOf(migrators);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread.ofVirtual().name("knowledge-schema-migrations").start(() -> {
            try {
                ensureMigrated();
                logger.info("Knowledge schemas are up to date");
            } catch (DatabaseUnavailableException exception) {
                logger.warn("Knowledge schema migration deferred to first use: {}", exception.getMessage());
            }
        });
    }

    /** Idempotent and thread-safe; throws {@link DatabaseUnavailableException} when it cannot complete. */
    public void ensureMigrated() {
        if (migrated) return;
        synchronized (this) {
            if (migrated) return;
            migrators.forEach(SchemaMigrator::ensureMigrated);
            migrated = true;
        }
    }
}
