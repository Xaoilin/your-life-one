package com.yourlifeone.knowledge.projects.domain;

/** A project found by name, catalogue key or tag, with just what Inventory needs to link to it. */
public record ProjectMatch(String id, String catalogKey, String name) {
}
