package com.yourlifeone.knowledge.knowledge.api.command;

import com.yourlifeone.knowledge.knowledge.api.request.TopicRequest;

/** Creates or replaces one topic. The queued payload carries the ID from the request path. */
public record SaveTopicCommand(String id, TopicRequest topic) {
}
