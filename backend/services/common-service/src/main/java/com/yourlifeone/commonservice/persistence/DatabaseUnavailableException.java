package com.yourlifeone.commonservice.persistence;

import com.yourlifeone.commonservice.web.ApiException;
import org.springframework.http.HttpStatus;

/** The service database is unconfigured, unreachable, or could not be migrated. */
public class DatabaseUnavailableException extends ApiException {
    public DatabaseUnavailableException(String detail, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "database_unavailable", "The database is unavailable.", detail, cause);
    }

    public DatabaseUnavailableException(String detail) {
        this(detail, null);
    }
}
