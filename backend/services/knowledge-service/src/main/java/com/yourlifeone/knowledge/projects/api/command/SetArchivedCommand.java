package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.knowledge.projects.api.request.ArchivedRequest;

/** Archives or unarchives one project. The queued payload carries the ID from the request path. */
public record SetArchivedCommand(String id, ArchivedRequest archived) {
}
