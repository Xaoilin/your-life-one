package com.yourlifeone.life.jobs.api.response;

/**
 * @param application null after a removal
 * @param duplicate true when the write matched an existing record and changed nothing
 */
public record ApplicationChangeResponse(String applicationId, JobApplicationResponse application, boolean duplicate) {
}
