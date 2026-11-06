package com.yourlifeone.commonservice.web;

import com.yourlifeone.commonservice.ratelimit.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps expected request failures to {@link ApiErrorResponse} bodies without leaking internals. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> apiException(ApiException exception) {
        if (exception.status().is5xxServerError()) {
            logger.error("API request failed: code={} detail={}", exception.code(),
                    exception.detail() == null ? exception.getMessage() : exception.detail(), exception.getCause());
        }
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    /** 429 with {@code Retry-After}, so clients wait before trying again. */
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiErrorResponse> rateLimited(RateLimitExceededException exception) {
        return ResponseEntity.status(exception.status())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
                .body(new ApiErrorResponse(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> invalidBody(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .findFirst()
                .orElse("Request body is invalid.");
        return error(HttpStatus.BAD_REQUEST, "invalid_request", message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> invalidParameter(HandlerMethodValidationException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", "Request parameters are invalid.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadableBody(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", "Request body is missing or malformed.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> missingParameter(MissingServletRequestParameterException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request",
                "Query parameter '" + exception.getParameterName() + "' is required.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request",
                "Parameter '" + exception.getName() + "' has an invalid value.");
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
