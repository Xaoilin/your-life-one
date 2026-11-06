package com.yourlifeone.finance.shared;

/** A write that names one record; the queued payload carries the ID from the request path. */
public record IdCommand(String id) {
}
