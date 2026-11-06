package com.yourlifeone.knowledge.projects.infrastructure.persistence.mapper;

import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectPage;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.entity.PageEntity;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.entity.ProjectEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface ProjectsPersistenceMapper {
    Project toDomain(ProjectEntity entity);

    ProjectEntity toEntity(UUID userId, Project project);

    ProjectPage toDomain(PageEntity entity);

    PageEntity toEntity(UUID userId, ProjectPage page);
}
