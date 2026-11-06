package com.yourlifeone.commonservice.persistence;

import com.yourlifeone.commonservice.web.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Reports an unreachable database as HTTP 503 instead of a generic server error. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DataAccessExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DataAccessExceptionHandler.class);

    @ExceptionHandler({
            DataAccessResourceFailureException.class,
            QueryTimeoutException.class,
            TransientDataAccessException.class,
            CannotCreateTransactionException.class
    })
    ResponseEntity<ApiErrorResponse> databaseUnavailable(RuntimeException exception) {
        logger.error("Database request failed: exceptionType={}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("database_unavailable", "The database is unavailable."));
    }
}
