package com.yourlifeone.profile.api.response;

/** @param accepted how many events were recorded; always the whole batch */
public record OperationalEventReceiptResponse(boolean ok, int accepted, int schemaVersion) {
}
