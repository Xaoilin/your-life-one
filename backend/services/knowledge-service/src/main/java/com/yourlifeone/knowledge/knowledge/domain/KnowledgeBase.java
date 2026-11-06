package com.yourlifeone.knowledge.knowledge.domain;

import java.util.List;

/** Every topic, in the user's order, and every entry, oldest first. */
public record KnowledgeBase(List<Topic> topics, List<Entry> entries) {
}
