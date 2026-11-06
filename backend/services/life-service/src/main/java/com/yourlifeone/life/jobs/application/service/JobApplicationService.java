package com.yourlifeone.life.jobs.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.jobs.config.JobsProperties;
import com.yourlifeone.life.jobs.domain.ApplicationChange;
import com.yourlifeone.life.jobs.domain.ApplicationPatch;
import com.yourlifeone.life.jobs.domain.HistoryEntry;
import com.yourlifeone.life.jobs.domain.JobApplication;
import com.yourlifeone.life.jobs.infrastructure.persistence.entity.JobApplicationEntity;
import com.yourlifeone.life.jobs.infrastructure.persistence.mapper.JobsPersistenceMapper;
import com.yourlifeone.life.jobs.infrastructure.persistence.repository.JobApplicationRepository;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.shared.Values;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job-tracker writes. Each changes exactly one application; a write that repeats an existing record
 * (same ID, or same link, company and role; same history ID or evidence link) changes nothing and
 * says so, so agents can safely report what they found more than once.
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository repository;
    private final JobsPersistenceMapper persistence;
    private final JobApplicationRules rules;
    private final JobsProperties properties;
    private final Clock clock;

    @Transactional
    public ApplicationChange add(UUID userId, JobApplication requested) {
        var application = rules.withDefaults(requested);
        var existing = repository.findByUserIdOrderByPosition(userId);
        var duplicate = existing.stream().filter(stored -> sameRecord(stored, application)).findFirst();
        if (duplicate.isPresent()) {
            return new ApplicationChange(duplicate.get().getId(), persistence.toDomain(duplicate.get()), true);
        }
        if (existing.size() >= properties.maxApplications()) {
            throw ApiException.badRequest("too_many_applications",
                    "At most " + properties.maxApplications() + " applications can be tracked.");
        }
        var now = Timestamps.now(clock);
        var position = existing.stream().mapToInt(JobApplicationEntity::getPosition).max().orElse(-1) + 1;
        var entity = persistence.toEntity(userId, position,
                application.toBuilder().createdAt(now).updatedAt(now).build());
        return changed(repository.save(entity));
    }

    @Transactional
    public ApplicationChange update(UUID userId, String id, ApplicationPatch patch) {
        var entity = find(userId, id);
        requireVersion(entity, patch.expectedUpdatedAt());
        var clear = patch.clear() == null ? List.<String>of() : patch.clear();
        rules.requireClearable(clear);
        var updated = applyPatch(persistence.toDomain(entity), patch, clear);
        rules.requireValid(updated);
        persistence.update(entity, updated.toBuilder().updatedAt(Timestamps.now(clock)).build());
        return changed(repository.save(entity));
    }

    @Transactional
    public ApplicationChange addHistory(UUID userId, String id, HistoryEntry requested) {
        var entity = find(userId, id);
        var entry = rules.entryWithDefaults(requested);
        var application = persistence.toDomain(entity);
        var repeated = application.history().stream().anyMatch(stored -> stored.id().equals(entry.id())
                || (entry.evidenceUrl() != null && entry.evidenceUrl().equals(stored.evidenceUrl())));
        if (repeated) {
            return new ApplicationChange(entity.getId(), application, true);
        }
        var history = new ArrayList<>(application.history());
        history.add(entry);
        var updated = application.toBuilder().history(history).build();
        rules.requireValid(updated);
        persistence.update(entity, updated.toBuilder().updatedAt(Timestamps.now(clock)).build());
        return changed(repository.save(entity));
    }

    @Transactional
    public ApplicationChange remove(UUID userId, String id, Instant expectedUpdatedAt) {
        var entity = find(userId, id);
        requireVersion(entity, expectedUpdatedAt);
        repository.delete(entity);
        return new ApplicationChange(entity.getId(), null, false);
    }

    private JobApplicationEntity find(UUID userId, String id) {
        return repository.findById(new UserScopedId(userId, RecordIds.require(id)))
                .orElseThrow(() -> ApiException.notFound("application_not_found", "This application does not exist."));
    }

    private static void requireVersion(JobApplicationEntity entity, Instant expectedUpdatedAt) {
        if (expectedUpdatedAt != null && !expectedUpdatedAt.equals(entity.getUpdatedAt())) {
            throw ApiException.conflict("application_changed", "This application changed; reload before saving.");
        }
    }

    private ApplicationChange changed(JobApplicationEntity saved) {
        return new ApplicationChange(saved.getId(), persistence.toDomain(saved), false);
    }

    /** The same ID, or the same job link for the same company and role. */
    private static boolean sameRecord(JobApplicationEntity stored, JobApplication candidate) {
        if (stored.getId().equals(candidate.id())) return true;
        return candidate.url() != null && candidate.url().equals(stored.getUrl())
                && stored.getCompany().toLowerCase(Locale.ROOT).equals(candidate.company().toLowerCase(Locale.ROOT))
                && stored.getRole().toLowerCase(Locale.ROOT).equals(candidate.role().toLowerCase(Locale.ROOT));
    }

    private static JobApplication applyPatch(JobApplication current, ApplicationPatch patch, List<String> clear) {
        return current.toBuilder()
                .company(patch.company() == null ? current.company() : patch.company().trim())
                .role(patch.role() == null ? current.role() : patch.role().trim())
                .url(clear.contains("url") ? null : Values.orElse(trimmed(patch.url()), current.url()))
                .workType(Values.orElse(patch.workType(), current.workType()))
                .remoteRegion(Values.orElse(patch.remoteRegion(), current.remoteRegion()))
                .remoteStatus(Values.orElse(patch.remoteStatus(), current.remoteStatus()))
                .remoteEvidence(Values.orElse(patch.remoteEvidence(), current.remoteEvidence()))
                .remoteCaveat(clear.contains("remoteCaveat") ? null
                        : Values.orElse(patch.remoteCaveat(), current.remoteCaveat()))
                .compensation(clear.contains("compensation") ? null
                        : Values.orElse(patch.compensation(), current.compensation()))
                .status(Values.orElse(patch.status(), current.status()))
                .applicationDate(clear.contains("applicationDate") ? null
                        : Values.orElse(patch.applicationDate(), current.applicationDate()))
                .nextAction(Values.orElse(patch.nextAction(), current.nextAction()))
                .nextActionDate(clear.contains("nextActionDate") ? null
                        : Values.orElse(patch.nextActionDate(), current.nextActionDate()))
                .notes(Values.orElse(patch.notes(), current.notes()))
                .build();
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
