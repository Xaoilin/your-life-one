package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.constraints.Size;

/** @param newItemId ID for the owned item created when the need links to none; chosen by the service when absent */
public record AcquireNeedRequest(@Size(max = 256) String newItemId) {
}
