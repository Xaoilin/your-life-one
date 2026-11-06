package com.yourlifeone.commonservice.web;

import org.springframework.http.HttpStatus;

/** An expected request failure returned to the caller as a bounded {@link ApiErrorResponse}. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String detail;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    /**
     * @param message safe text returned to the caller
     * @param detail operator-facing text written to logs only; may be null
     */
    public ApiException(HttpStatus status, String code, String message, String detail, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String detail() {
        return detail;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
