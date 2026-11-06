package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.life.inventory.api.request.NeedRequest;

/** Creates or replaces one need; the queued payload carries the ID from the request path. */
public record SaveNeedCommand(String id, NeedRequest need) {
}
