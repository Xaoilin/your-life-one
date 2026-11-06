package com.yourlifeone.knowledge.knowledge.api.response;

import java.util.List;

/** Every topic in the user's order and every entry, oldest first. */
public record KnowledgeResponse(List<TopicResponse> topics, List<EntryResponse> entries) {
}
