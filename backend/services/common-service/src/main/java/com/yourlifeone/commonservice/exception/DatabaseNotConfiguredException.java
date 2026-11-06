package com.yourlifeone.commonservice.exception;

/** Identifies a database health check that is missing required runtime configuration. */
public final class DatabaseNotConfiguredException extends DatabaseHealthCheckException {
    public DatabaseNotConfiguredException(String databaseName, String configurationProperty) {
        super(databaseName + " database is not configured: " + configurationProperty + " is missing");
    }
}
