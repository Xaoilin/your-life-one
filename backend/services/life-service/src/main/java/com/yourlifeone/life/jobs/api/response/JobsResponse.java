package com.yourlifeone.life.jobs.api.response;

import java.util.List;

/** Every application the user tracks, in the order they were added. */
public record JobsResponse(List<JobApplicationResponse> applications) {
}
