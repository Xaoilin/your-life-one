package com.yourlifeone.profile.activity.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Events sent together by the React app; the allowed batch size is {@code app.profile.activity.ingest.max-batch}. */
public record ActivityEventBatchRequest(@NotNull List<@NotNull @Valid ProductUsageEventRequest> events) {
}
