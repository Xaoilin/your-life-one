package com.yourlifeone.knowledge.knowledge.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One cited source; the type is checked against configuration. */
public record SourceRequest(
        @NotNull @Size(max = 32) String type,
        @Min(1) @Max(114) Integer surah,
        @Min(1) @Max(286) Integer ayahStart,
        @Min(1) @Max(286) Integer ayahEnd,
        @Size(max = 120) String collection,
        @Size(max = 40) String hadithNumber,
        @Size(max = 160) String author,
        @Size(max = 300) String title,
        @Size(max = 2048) String url) {
}
