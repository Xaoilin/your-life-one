package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.request.UpdateAccountRequest;

/** Changes one account. The queued payload carries the ID from the request path. */
public record UpdateAccountCommand(String accountId, UpdateAccountRequest changes) {
}
