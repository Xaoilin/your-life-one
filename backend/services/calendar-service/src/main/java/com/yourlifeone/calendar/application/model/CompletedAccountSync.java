package com.yourlifeone.calendar.application.model;

/** A successful provider pass and its process-local completion order. Contains no credentials. */
public record CompletedAccountSync(long sequence, AccountSyncResult result) {
}
