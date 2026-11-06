package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Items to create or enrich together, all or none (at most {@code app.life.inventory.max-batch}). */
public record SaveItemsRequest(@NotEmpty List<@Valid ItemCandidateRequest> items) {
}
