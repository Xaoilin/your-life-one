package com.yourlifeone.commonservice.exception;

/** Base exception for a service database health check that cannot complete. */
public class DatabaseHealthCheckException extends RuntimeException {
    public DatabaseHealthCheckException(String message) {
        super(message);
    }

    public DatabaseHealthCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
