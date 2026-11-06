package com.yourlifeone.life.jobs.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed values and limits of the job tracker ({@code app.life.jobs.*}).
 *
 * @param clearableFields optional fields an update may clear, e.g. {@code url}
 * @param defaultRemoteEvidence remote evidence for a new application that has none yet
 * @param defaultNextAction next action for a new application that has none yet
 * @param maxApplications most applications one user may keep
 * @param maxHistory most history entries one application may keep
 * @param maxPageSize largest page a search returns
 */
@ConfigurationProperties("app.life.jobs")
public record JobsProperties(
        List<String> statuses,
        List<String> workTypes,
        List<String> remoteRegions,
        List<String> remoteStatuses,
        List<String> historyKinds,
        List<String> clearableFields,
        String defaultRemoteEvidence,
        String defaultNextAction,
        int maxApplications,
        int maxHistory,
        int maxPageSize) {
    public JobsProperties {
        statuses = List.copyOf(statuses);
        workTypes = List.copyOf(workTypes);
        remoteRegions = List.copyOf(remoteRegions);
        remoteStatuses = List.copyOf(remoteStatuses);
        historyKinds = List.copyOf(historyKinds);
        clearableFields = clearableFields == null ? List.of() : List.copyOf(clearableFields);
    }
}
