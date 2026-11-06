package com.yourlifeone.knowledge.projects.api.mapper;

import com.yourlifeone.knowledge.projects.api.request.NewProjectRequest;
import com.yourlifeone.knowledge.projects.api.request.PageRequest;
import com.yourlifeone.knowledge.projects.api.request.ProjectReferenceRequest;
import com.yourlifeone.knowledge.projects.api.response.PageResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectCatalogueResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectMatchResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectMatchesResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectResponse;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectCatalogue;
import com.yourlifeone.knowledge.projects.domain.ProjectMatch;
import com.yourlifeone.knowledge.projects.domain.ProjectPage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProjectsMapper {
    ProjectCatalogueResponse toResponse(ProjectCatalogue catalogue);

    @Mapping(target = "isPinned", source = "pinned")
    ProjectResponse toResponse(Project project);

    @Mapping(target = "isOverview", source = "overview")
    PageResponse toResponse(ProjectPage page);

    ProjectMatchResponse toResponse(ProjectMatch match);

    List<ProjectMatchResponse> toMatchResponses(List<ProjectMatch> matches);

    default ProjectMatchesResponse toMatchesResponse(List<ProjectMatch> matches) {
        return new ProjectMatchesResponse(toMatchResponses(matches));
    }

    @Mapping(target = "pinned", source = "isPinned")
    @Mapping(target = "statusBeforeArchive", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toDomain(NewProjectRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "catalogKey", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusBeforeArchive", ignore = true)
    @Mapping(target = "pinned", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toDomain(ProjectReferenceRequest request);

    @Mapping(target = "overview", source = "request.isOverview")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProjectPage toDomain(String id, PageRequest request);
}
