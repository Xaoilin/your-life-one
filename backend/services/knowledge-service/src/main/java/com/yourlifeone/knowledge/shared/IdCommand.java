package com.yourlifeone.knowledge.shared;

/** A write that names one record; the queued payload carries the ID from the request path. */
public record IdCommand(String id) {
}
