package com.yourlifeone.finance.equity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.equity.config.EquityProperties;
import com.yourlifeone.finance.equity.domain.EquityPositionDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * A position's rules beyond each field's own (annotations on the draft): the company name is trimmed and
 * the whole position is bounded. Search pages are bounded too.
 */
@Component
@RequiredArgsConstructor
public class EquityRules {
    private final EquityProperties properties;
    private final JsonMapper json;

    public EquityPositionDraft check(EquityPositionDraft draft) {
        var tidied = draft.toBuilder().company(draft.company().trim()).build();
        if (json.writeValueAsBytes(tidied).length > properties.maxDocumentBytes()) {
            throw ApiException.badRequest("invalid_position",
                    "The position must fit within " + properties.maxDocumentBytes() + " bytes.");
        }
        return tidied;
    }

    /** The search text, trimmed; empty matches every position. */
    public String query(String query) {
        var trimmed = query == null ? "" : query.trim();
        if (trimmed.length() > properties.maxQueryLength()) {
            throw invalidPage("query must be at most " + properties.maxQueryLength() + " characters.");
        }
        return trimmed;
    }

    public int limit(Integer limit) {
        var size = limit == null ? properties.defaultPageSize() : limit;
        if (size < 1 || size > properties.maxPageSize()) {
            throw invalidPage("limit must be between 1 and " + properties.maxPageSize() + ".");
        }
        return size;
    }

    public int offset(Integer offset) {
        var start = offset == null ? 0 : offset;
        if (start < 0 || start > properties.maxOffset()) {
            throw invalidPage("offset must be between 0 and " + properties.maxOffset() + ".");
        }
        return start;
    }

    private static ApiException invalidPage(String message) {
        return ApiException.badRequest("invalid_page", message);
    }
}
