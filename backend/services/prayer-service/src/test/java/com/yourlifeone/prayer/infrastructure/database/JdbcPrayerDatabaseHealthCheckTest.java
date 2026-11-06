package com.yourlifeone.prayer.infrastructure.database;

import com.yourlifeone.commonservice.exception.DatabaseHealthCheckException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPrayerDatabaseHealthCheckTest {
    @Test
    void executesSelectOneAndReportsTheExpectedResult() {
        var jdbcTemplate = new StubJdbcTemplate(1, null);
        var healthCheck = new JdbcPrayerDatabaseHealthCheck(jdbcTemplate);

        assertTrue(healthCheck.isDatabaseAvailable());
        assertTrue(jdbcTemplate.wasCalledWithSelectOne());
    }

    @Test
    void wrapsTheQueryFailureWithTheOriginalCause() {
        var cause = new DataAccessResourceFailureException("database unavailable");
        var jdbcTemplate = new StubJdbcTemplate(null, cause);
        var healthCheck = new JdbcPrayerDatabaseHealthCheck(jdbcTemplate);

        var exception = assertThrows(DatabaseHealthCheckException.class, healthCheck::isDatabaseAvailable);

        assertSame(cause, exception.getCause());
        assertTrue(jdbcTemplate.wasCalledWithSelectOne());
    }

    @Test
    void reportsDownIfTheQueryDoesNotReturnOne() {
        var healthCheck = new JdbcPrayerDatabaseHealthCheck(new StubJdbcTemplate(2, null));

        assertFalse(healthCheck.isDatabaseAvailable());
    }

    private static final class StubJdbcTemplate extends JdbcTemplate {
        private final Integer result;
        private final RuntimeException error;
        private boolean calledWithSelectOne;

        private StubJdbcTemplate(Integer result, RuntimeException error) {
            this.result = result;
            this.error = error;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            calledWithSelectOne = "SELECT 1".equals(sql);
            if (error != null) throw error;
            return requiredType.cast(result);
        }

        private boolean wasCalledWithSelectOne() {
            return calledWithSelectOne;
        }
    }
}
