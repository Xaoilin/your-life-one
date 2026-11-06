package com.yourlifeone.life.jobs.domain;

import lombok.Builder;

/**
 * What an agent looks for: text anywhere in an application or its history, and optional filters.
 *
 * @param query case-insensitive text; blank matches everything
 */
@Builder
public record ApplicationSearch(String query, String status, String workType, String remoteStatus, int limit,
        int offset) {
}
