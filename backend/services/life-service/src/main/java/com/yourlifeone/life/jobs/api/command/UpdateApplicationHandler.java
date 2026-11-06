package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.jobs.api.mapper.JobsMapper;
import com.yourlifeone.life.jobs.application.service.JobApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Changes an application from the command queue. */
@Component
@RequiredArgsConstructor
public class UpdateApplicationHandler implements WriteCommandHandler<UpdateApplicationCommand> {
    public static final String TYPE = "jobs.update-application";

    private final JobApplicationService applications;
    private final JobsMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<UpdateApplicationCommand> payloadType() {
        return UpdateApplicationCommand.class;
    }

    @Override
    public Object handle(UUID userId, UpdateApplicationCommand command) {
        return mapper.toResponse(applications.update(userId, command.applicationId(), mapper.toDomain(command.patch())));
    }
}
