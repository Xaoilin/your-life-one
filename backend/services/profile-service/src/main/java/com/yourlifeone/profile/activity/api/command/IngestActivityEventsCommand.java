package com.yourlifeone.profile.activity.api.command;

import com.yourlifeone.profile.activity.api.request.ProductUsageEventRequest;
import java.util.List;

/** A batch of product-usage events, as queued. */
public record IngestActivityEventsCommand(List<ProductUsageEventRequest> events) {
}
