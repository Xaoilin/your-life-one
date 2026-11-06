package com.yourlifeone.knowledge.lifestyle.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.lifestyle.config.LifestyleProperties;
import com.yourlifeone.knowledge.shared.AllowedValues;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Which columns and statuses exist, and which statuses belong to which column. */
@Component
@RequiredArgsConstructor
public class LifestyleRules {
    private final LifestyleProperties properties;

    public String requireType(String type) {
        return AllowedValues.require("type", type, types());
    }

    /** The status, which must belong to the type's family. */
    public String requireStatus(String type, String status) {
        return AllowedValues.require("status", status, statusesFor(type));
    }

    /** The status an item keeps when it moves to another column: unchanged within a family, else the start. */
    public String statusAfterMove(String fromType, String toType, String status) {
        return isPractice(fromType) == isPractice(toType) ? status : statusesFor(toType).getFirst();
    }

    public List<String> sources(List<String> sources) {
        if (sources.size() > properties.maxSources()) {
            throw ApiException.badRequest("too_many_sources",
                    "An item may keep at most " + properties.maxSources() + " references.");
        }
        return sources;
    }

    private List<String> types() {
        return Stream.concat(properties.avoidTypes().stream(), properties.practiceTypes().stream()).toList();
    }

    private List<String> statusesFor(String type) {
        return isPractice(type) ? properties.practiceStatuses() : properties.avoidStatuses();
    }

    private boolean isPractice(String type) {
        return properties.practiceTypes().contains(type);
    }
}
