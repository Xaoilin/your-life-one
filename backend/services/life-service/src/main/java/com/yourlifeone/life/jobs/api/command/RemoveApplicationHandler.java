package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.jobs.api.mapper.JobsMapper;
import com.yourlifeone.life.jobs.application.service.JobApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Removes one application and its history from the command queue. */
@Component
@RequiredArgsConstructor
public class RemoveApplicationHandler implements WriteCommandHandler<RemoveApplicationCommand> {
    public static final String TYPE = "jobs.remove-application";

    private final JobApplicationService applications;
    private final JobsMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<RemoveApplicationCommand> payloadType() {
        return RemoveApplicationCommand.class;
    }

    @Override
    public Object handle(UUID userId, RemoveApplicationCommand command) {
        return mapper.toResponse(applications.remove(userId, command.applicationId(), command.expectedUpdatedAt()));
    }
}
