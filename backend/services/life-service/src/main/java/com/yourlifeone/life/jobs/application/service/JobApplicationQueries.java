package com.yourlifeone.life.jobs.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.jobs.config.JobsProperties;
import com.yourlifeone.life.jobs.domain.ApplicationPage;
import com.yourlifeone.life.jobs.domain.ApplicationSearch;
import com.yourlifeone.life.jobs.domain.JobApplication;
import com.yourlifeone.life.jobs.infrastructure.persistence.mapper.JobsPersistenceMapper;
import com.yourlifeone.life.jobs.infrastructure.persistence.repository.JobApplicationRepository;
import com.yourlifeone.life.shared.AllowedValues;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.UserScopedId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Reads the job tracker: whole for the app, searched and paged for agents. */
@Service
@RequiredArgsConstructor
public class JobApplicationQueries {
    private static final int MAX_QUERY_LENGTH = 160;
    private static final int MAX_OFFSET = 10_000;

    private final JobApplicationRepository repository;
    private final JobsPersistenceMapper persistence;
    private final JobsProperties properties;

    @Transactional(readOnly = true)
    public List<JobApplication> all(UUID userId) {
        return repository.findByUserIdOrderByPosition(userId).stream().map(persistence::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public JobApplication get(UUID userId, String id) {
        return repository.findById(new UserScopedId(userId, RecordIds.require(id))).map(persistence::toDomain)
                .orElseThrow(() -> ApiException.notFound("application_not_found", "This application does not exist."));
    }

    /** Text matches anywhere in an application or its history, ignoring case. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ApplicationPage search(UUID userId, ApplicationSearch search) {
        requireBounds(search);
        var needle = search.query().trim().toLowerCase(Locale.ROOT);
        var ids = repository.searchIds(userId, needle, search.status(), search.workType(), search.remoteStatus(),
                search.limit(), search.offset());
        var total = repository.countMatches(userId, needle, search.status(), search.workType(), search.remoteStatus());
        var page = ids.isEmpty() ? List.<JobApplication>of() : repository.fetchSelected(userId, ids).stream()
                .map(persistence::toDomain).toList();
        return new ApplicationPage(page, total, search.limit(), search.offset());
    }

    private void requireBounds(ApplicationSearch search) {
        if (search.query().length() > MAX_QUERY_LENGTH || search.limit() < 1
                || search.limit() > properties.maxPageSize() || search.offset() < 0 || search.offset() > MAX_OFFSET) {
            throw ApiException.badRequest("invalid_search", "Search query, limit or offset is out of bounds.");
        }
        AllowedValues.optional("status", search.status(), properties.statuses());
        AllowedValues.optional("workType", search.workType(), properties.workTypes());
        AllowedValues.optional("remoteStatus", search.remoteStatus(), properties.remoteStatuses());
    }

}
