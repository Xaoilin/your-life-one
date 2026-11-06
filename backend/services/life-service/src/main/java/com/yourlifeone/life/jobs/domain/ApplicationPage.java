package com.yourlifeone.life.jobs.domain;

import java.util.List;

/** One page of search results; {@code total} counts every match. */
public record ApplicationPage(List<JobApplication> applications, int total, int limit, int offset) {
}
