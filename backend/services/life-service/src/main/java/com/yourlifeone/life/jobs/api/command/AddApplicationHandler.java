package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.jobs.api.mapper.JobsMapper;
import com.yourlifeone.life.jobs.api.request.NewApplicationRequest;
import com.yourlifeone.life.jobs.application.service.JobApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Records a new application from the command queue, or reports the existing one it duplicates. */
@Component
@RequiredArgsConstructor
public class AddApplicationHandler implements WriteCommandHandler<NewApplicationRequest> {
    public static final String TYPE = "jobs.add-application";

    private final JobApplicationService applications;
    private final JobsMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<NewApplicationRequest> payloadType() {
        return NewApplicationRequest.class;
    }

    @Override
    public Object handle(UUID userId, NewApplicationRequest request) {
        return mapper.toResponse(applications.add(userId, mapper.toDomain(request)));
    }
}
