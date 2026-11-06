package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.jobs.api.mapper.JobsMapper;
import com.yourlifeone.life.jobs.application.service.JobApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Appends evidence from the command queue; a repeated entry ID or link changes nothing. */
@Component
@RequiredArgsConstructor
public class AddHistoryHandler implements WriteCommandHandler<AddHistoryCommand> {
    public static final String TYPE = "jobs.add-history";

    private final JobApplicationService applications;
    private final JobsMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<AddHistoryCommand> payloadType() {
        return AddHistoryCommand.class;
    }

    @Override
    public Object handle(UUID userId, AddHistoryCommand command) {
        return mapper.toResponse(applications.addHistory(userId, command.applicationId(),
                mapper.toDomain(command.entry())));
    }
}
