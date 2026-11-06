package com.yourlifeone.profile.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Up to ten operational events, sent together by the React app. */
public record OperationalEventBatchRequest(@NotNull @Size(min = 1, max = 10) List<@NotNull @Valid OperationalEventRequest> events) {
}
