package com.yourlifeone.commonservice.command.retention;

import com.yourlifeone.commonservice.command.WriteCommandProperties;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Keeps the processed-command table bounded: remembered writes older than the retention are
 * deleted. A database outage only postpones cleanup to the next run.
 */
public class ProcessedCommandCleanup {
    private static final Logger logger = LoggerFactory.getLogger(ProcessedCommandCleanup.class);

    private final ProcessedCommandRepository processed;
    private final WriteCommandProperties properties;

    public ProcessedCommandCleanup(ProcessedCommandRepository processed, WriteCommandProperties properties) {
        this.processed = processed;
        this.properties = properties;
    }

    @Scheduled(initialDelayString = "${app.write-commands.cleanup-initial-delay:PT5M}",
            fixedDelayString = "${app.write-commands.cleanup-interval:PT6H}")
    public void deleteExpired() {
        try {
            var deleted = processed.deleteOlderThan(properties.retention());
            if (deleted > 0) {
                logger.info("Processed-command cleanup: deleted={} retention={}", deleted, properties.retention());
            }
        } catch (DataAccessException exception) {
            logger.warn("Processed-command cleanup postponed: exceptionType={}", exception.getClass().getSimpleName());
        }
    }
}
