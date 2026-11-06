package com.yourlifeone.knowledge.progress.domain;

import lombok.Builder;

/**
 * How far the user is in the knowledge base and the lifestyle tracker, as counts: what the planner's
 * Islamic badges look at.
 *
 * @param haramMastered things avoided that reached the mastered status
 * @param halalConsistent good habits that reached the consistent status
 */
@Builder
public record KnowledgeProgress(long entries, long topics, long haramMastered, long halalConsistent,
        long lifestyleItems) {
}
