package com.yourlifeone.knowledge.knowledge.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.knowledge.config.KnowledgeProperties;
import com.yourlifeone.knowledge.knowledge.domain.KnowledgeSource;
import com.yourlifeone.knowledge.shared.AllowedValues;
import com.yourlifeone.knowledge.shared.Texts;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Checks and tidies what an entry cites and how it is tagged. */
@Component
@RequiredArgsConstructor
public class KnowledgeRules {
    private final KnowledgeProperties properties;

    /** Sources with a configured type; blank text fields become absent. */
    public List<KnowledgeSource> sources(List<KnowledgeSource> requested) {
        var sources = requested == null ? List.<KnowledgeSource>of() : requested;
        if (sources.size() > properties.maxSources()) {
            throw ApiException.badRequest("too_many_sources",
                    "An entry may cite at most " + properties.maxSources() + " sources.");
        }
        return sources.stream().map(this::source).toList();
    }

    /** Distinct trimmed tags, within the configured limit. */
    public List<String> tags(List<String> requested) {
        var tags = Texts.distinctTrimmed(requested);
        if (tags.size() > properties.maxTags()) {
            throw ApiException.badRequest("too_many_tags", "An entry may carry at most " + properties.maxTags() + " tags.");
        }
        return tags;
    }

    private KnowledgeSource source(KnowledgeSource source) {
        AllowedValues.require("sourceType", source.type(), properties.sourceTypes());
        return source.toBuilder()
                .collection(Texts.blankToNull(source.collection()))
                .hadithNumber(Texts.blankToNull(source.hadithNumber()))
                .author(Texts.blankToNull(source.author()))
                .title(Texts.blankToNull(source.title()))
                .url(Texts.blankToNull(source.url()))
                .build();
    }
}
