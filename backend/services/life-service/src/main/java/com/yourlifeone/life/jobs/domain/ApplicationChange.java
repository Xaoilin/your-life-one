package com.yourlifeone.life.jobs.domain;

/**
 * The outcome of a job-tracker write.
 *
 * @param application the application as stored; null after a removal
 * @param duplicate true when the write matched an existing record and changed nothing
 */
public record ApplicationChange(String applicationId, JobApplication application, boolean duplicate) {
}
