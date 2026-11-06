package com.yourlifeone.knowledge.progress.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which lifestyle columns and statuses the progress counts ({@code app.knowledge.progress.*}).
 *
 * @param haramType the column of forbidden things, e.g. {@code haram}
 * @param masteredStatus the status of a forbidden thing given up for good, e.g. {@code mastered}
 * @param halalType the column of good habits, e.g. {@code halal}
 * @param consistentStatus the status of a habit kept up, e.g. {@code consistent}
 */
@ConfigurationProperties("app.knowledge.progress")
public record ProgressProperties(String haramType, String masteredStatus, String halalType, String consistentStatus) {
}
