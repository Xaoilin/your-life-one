package com.yourlifeone.life.jobs.api.response;

import java.util.List;

public record ApplicationPageResponse(List<JobApplicationResponse> applications, int total, int limit, int offset) {
}
