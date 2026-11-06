package com.yourlifeone.knowledge.knowledge.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The whole entry; the ID is in the path. Changing the topic moves the entry. */
public record EntryRequest(
        @NotBlank @Size(max = 256) String topicId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 100000) String content,
        @NotNull List<@Valid @NotNull SourceRequest> sources,
        @NotNull List<@NotNull @Size(max = 50) String> tags) {
}
