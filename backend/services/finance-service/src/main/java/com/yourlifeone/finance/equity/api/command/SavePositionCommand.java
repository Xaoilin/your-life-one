package com.yourlifeone.finance.equity.api.command;

import com.yourlifeone.finance.equity.api.request.SavePositionRequest;

/** Creates or replaces one position. The queued payload carries the ID from the request path. */
public record SavePositionCommand(String id, SavePositionRequest request) {
}
