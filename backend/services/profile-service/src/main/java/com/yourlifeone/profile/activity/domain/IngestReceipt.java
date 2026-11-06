package com.yourlifeone.profile.activity.domain;

/**
 * The result of storing a batch: every event is either newly stored or already known.
 *
 * @param accepted events stored by this batch
 * @param duplicates events that were already stored (or repeated within the batch)
 */
public record IngestReceipt(int accepted, int duplicates) {
}
