package com.yourlifeone.life.infrastructure.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs a read-only {@code SELECT 1}; reports DOWN (never throws) when the database is unreachable. */
public class LifeDatabaseHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LifeDatabaseHealthCheck.class);

    private final JdbcTemplate jdbcTemplate;

    public LifeDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseAvailable() {
        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException exception) {
            logger.error("Life database health check failed: exceptionType={}",
                    exception.getClass().getSimpleName(), exception);
            return false;
        }
    }
}
