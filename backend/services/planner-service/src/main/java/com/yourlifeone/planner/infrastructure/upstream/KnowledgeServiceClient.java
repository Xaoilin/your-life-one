package com.yourlifeone.planner.infrastructure.upstream;

import com.yourlifeone.planner.config.UpstreamProperties;
import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.infrastructure.upstream.dto.KnowledgeProgressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads from the knowledge service as the signed-in user, forwarding their access token. */
@Component
@RequiredArgsConstructor
public class KnowledgeServiceClient {
    static final String PROGRESS = "/api/knowledge/v1/progress";
    private static final String BEARER = "Bearer ";

    private final RestClient upstreamRestClient;
    private final UpstreamProperties properties;
    private final UpstreamMapper mapper;

    /**
     * The user's knowledge entries, topics and lifestyle items, counted.
     *
     * @throws UpstreamUnavailableException when the knowledge service does not answer with them
     */
    public KnowledgeProgress progress(String accessToken) {
        try {
            var body = upstreamRestClient.get()
                    .uri(properties.knowledgeUrl() + PROGRESS)
                    .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                    .retrieve()
                    .body(KnowledgeProgressDto.class);
            if (body == null) throw new RestClientException("empty answer");
            return mapper.toDomain(body);
        } catch (RestClientException exception) {
            throw new UpstreamUnavailableException("Knowledge service unavailable: " + exception.getMessage(), exception);
        }
    }
}
