package com.yourlifeone.commonservice.command;

import org.springframework.http.HttpStatus;

/**
 * A completed write as the API returns it.
 *
 * @param replayed true when this is the stored result of an earlier request with the same idempotency key
 */
public record WriteCommandResult<T>(HttpStatus status, T body, boolean replayed) {
}
