package com.yourlifeone.profile.activity.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.profile.activity.api.mapper.ActivityMapper;
import com.yourlifeone.profile.activity.application.service.ActivityIngestService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Stores a batch of product-usage events from the command queue. */
@Component
@RequiredArgsConstructor
public class IngestActivityEventsHandler implements WriteCommandHandler<IngestActivityEventsCommand> {
    public static final String TYPE = "profile.ingest-activity-events";

    private final ActivityIngestService ingest;
    private final ActivityMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<IngestActivityEventsCommand> payloadType() {
        return IngestActivityEventsCommand.class;
    }

    @Override
    public Object handle(UUID userId, IngestActivityEventsCommand command) {
        return mapper.toResponse(ingest.ingest(userId, mapper.toDomain(command.events())));
    }
}
