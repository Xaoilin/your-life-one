package com.yourlifeone.profile.infrastructure.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs a read-only {@code SELECT 1}; reports DOWN (never throws) when the database is unreachable. */
public class ProfileDatabaseHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(ProfileDatabaseHealthCheck.class);

    private final JdbcTemplate jdbcTemplate;

    public ProfileDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseAvailable() {
        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException exception) {
            logger.error("Profile database health check failed: exceptionType={}",
                    exception.getClass().getSimpleName(), exception);
            return false;
        }
    }
}
