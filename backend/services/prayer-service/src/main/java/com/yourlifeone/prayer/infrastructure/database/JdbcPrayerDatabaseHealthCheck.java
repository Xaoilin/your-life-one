package com.yourlifeone.prayer.infrastructure.database;

import com.yourlifeone.prayer.application.port.PrayerDatabaseHealthCheck;
import com.yourlifeone.commonservice.exception.DatabaseHealthCheckException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcPrayerDatabaseHealthCheck implements PrayerDatabaseHealthCheck {
    private static final String DATABASE_PING_SQL = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public JdbcPrayerDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isDatabaseAvailable() {
        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject(DATABASE_PING_SQL, Integer.class));
        } catch (DataAccessException exception) {
            throw new DatabaseHealthCheckException("Prayer database query failed", exception);
        }
    }
}
