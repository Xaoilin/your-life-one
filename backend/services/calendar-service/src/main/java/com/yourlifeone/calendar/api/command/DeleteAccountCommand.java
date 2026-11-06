package com.yourlifeone.calendar.api.command;

/** Deletes one account with its calendars and events. The queued payload carries the ID from the request path. */
public record DeleteAccountCommand(String accountId) {
}
