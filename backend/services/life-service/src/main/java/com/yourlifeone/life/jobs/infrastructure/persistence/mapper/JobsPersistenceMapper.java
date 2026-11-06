package com.yourlifeone.life.jobs.infrastructure.persistence.mapper;

import com.yourlifeone.life.jobs.domain.HistoryEntry;
import com.yourlifeone.life.jobs.domain.JobApplication;
import com.yourlifeone.life.jobs.infrastructure.persistence.entity.HistoryEntryEmbeddable;
import com.yourlifeone.life.jobs.infrastructure.persistence.entity.JobApplicationEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface JobsPersistenceMapper {
    JobApplication toDomain(JobApplicationEntity entity);

    HistoryEntry toDomain(HistoryEntryEmbeddable entry);

    HistoryEntryEmbeddable toEmbeddable(HistoryEntry entry);

    @Mapping(target = "history", expression = "java(toEmbeddables(application.history()))")
    JobApplicationEntity toEntity(UUID userId, int position, JobApplication application);

    /** Copies every field but the owner, ID and position, replacing the history in place. */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "history", ignore = true)
    void copy(JobApplication application, @MappingTarget JobApplicationEntity entity);

    default void update(JobApplicationEntity entity, JobApplication application) {
        copy(application, entity);
        entity.getHistory().clear();
        entity.getHistory().addAll(toEmbeddables(application.history()));
    }

    default List<HistoryEntryEmbeddable> toEmbeddables(List<HistoryEntry> history) {
        return new ArrayList<>(history == null ? List.of() : history.stream().map(this::toEmbeddable).toList());
    }
}
