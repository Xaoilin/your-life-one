package com.yourlifeone.life.jobs.api.mapper;

import com.yourlifeone.life.jobs.api.request.ApplicationPatchRequest;
import com.yourlifeone.life.jobs.api.request.HistoryEntryRequest;
import com.yourlifeone.life.jobs.api.request.NewApplicationRequest;
import com.yourlifeone.life.jobs.api.response.ApplicationChangeResponse;
import com.yourlifeone.life.jobs.api.response.ApplicationPageResponse;
import com.yourlifeone.life.jobs.api.response.JobApplicationResponse;
import com.yourlifeone.life.jobs.domain.ApplicationChange;
import com.yourlifeone.life.jobs.domain.ApplicationPage;
import com.yourlifeone.life.jobs.domain.ApplicationPatch;
import com.yourlifeone.life.jobs.domain.HistoryEntry;
import com.yourlifeone.life.jobs.domain.JobApplication;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface JobsMapper {
    JobApplicationResponse toResponse(JobApplication application);

    List<JobApplicationResponse> toResponses(List<JobApplication> applications);

    ApplicationPageResponse toResponse(ApplicationPage page);

    ApplicationChangeResponse toResponse(ApplicationChange change);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobApplication toDomain(NewApplicationRequest request);

    ApplicationPatch toDomain(ApplicationPatchRequest request);

    HistoryEntry toDomain(HistoryEntryRequest request);
}
