package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.infrastructure.upstream.KnowledgeServiceClient;
import com.yourlifeone.planner.infrastructure.upstream.UpstreamUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * The knowledge counts some badges need, fetched in the request (with the caller's token) so the queued
 * write carries them. A completion never waits on the knowledge service: without the counts, the badges that
 * need them are skipped this time.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeProgressSource {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeProgressSource.class);

    private final KnowledgeServiceClient knowledge;

    /** The counts, or null (with a warning) when the knowledge service does not answer. */
    public KnowledgeProgress progressOrNull(Jwt caller) {
        try {
            return knowledge.progress(caller.getTokenValue());
        } catch (UpstreamUnavailableException exception) {
            logger.warn("Knowledge badges skipped: {}", exception.getMessage());
            return null;
        }
    }
}
