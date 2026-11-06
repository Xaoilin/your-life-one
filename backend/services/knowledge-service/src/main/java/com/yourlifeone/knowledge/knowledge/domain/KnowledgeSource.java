package com.yourlifeone.knowledge.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * Where a knowledge entry's point comes from. Which fields apply depends on the type: a Quran verse
 * range, a hadith by collection and number, or a scholarly or other work by author, title and link.
 *
 * @param type one of the configured source types, e.g. {@code quran}
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeSource(
        String type,
        Integer surah,
        Integer ayahStart,
        Integer ayahEnd,
        String collection,
        String hadithNumber,
        String author,
        String title,
        String url) {
}
