package com.yourlifeone.profile.activity.api.response;

/**
 * What happened to a batch: {@code accepted + duplicates} is always the number of events sent.
 *
 * @param duplicates events already stored, which are not stored again
 */
public record ActivityIngestReceiptResponse(int accepted, int duplicates) {
}
