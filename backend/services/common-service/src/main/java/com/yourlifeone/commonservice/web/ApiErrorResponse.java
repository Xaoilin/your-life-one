package com.yourlifeone.commonservice.web;

/** Error body for every API failure: a stable machine-readable code and a safe human message. */
public record ApiErrorResponse(String code, String message) {
}
