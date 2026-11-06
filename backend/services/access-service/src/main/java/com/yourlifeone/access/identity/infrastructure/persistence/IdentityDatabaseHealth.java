package com.yourlifeone.access.identity.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Read-only database availability check; no account records are loaded. */
@Component
@RequiredArgsConstructor
public class IdentityDatabaseHealth {
    private final JdbcTemplate database;

    public void check() {
        database.queryForObject("select 1", Integer.class);
    }
}
