package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.knowledge.projects.api.request.PageRequest;

/** Creates or replaces one wiki page. The queued payload carries the ID from the request path. */
public record SavePageCommand(String id, PageRequest page) {
}
