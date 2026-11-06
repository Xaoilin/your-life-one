package com.yourlifeone.planner.tasks.infrastructure.persistence.mapper;

import com.yourlifeone.planner.tasks.domain.Task;
import com.yourlifeone.planner.tasks.infrastructure.persistence.entity.TaskEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface TasksPersistenceMapper {
    Task toDomain(TaskEntity entity);

    TaskEntity toEntity(UUID userId, Task task);
}
