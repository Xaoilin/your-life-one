package com.yourlifeone.knowledge.knowledge.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knowledge-base rules ({@code app.knowledge.knowledge.*}).
 *
 * @param sourceTypes the kinds of source an entry may cite, e.g. {@code quran}
 * @param maxSources the most sources one entry may cite
 * @param maxTags the most tags one entry may carry
 */
@ConfigurationProperties("app.knowledge.knowledge")
public record KnowledgeProperties(List<String> sourceTypes, int maxSources, int maxTags) {
    public KnowledgeProperties {
        sourceTypes = sourceTypes == null ? List.of() : List.copyOf(sourceTypes);
    }
}
