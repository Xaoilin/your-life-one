package com.yourlifeone.life.health.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.health.application.service.FastFoodService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Deletes a journal entry from the command queue. */
@Component
@RequiredArgsConstructor
public class DeleteFastFoodEntryHandler implements WriteCommandHandler<DeleteFastFoodEntryCommand> {
    public static final String TYPE = "health.delete-fast-food-entry";

    private final FastFoodService entries;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<DeleteFastFoodEntryCommand> payloadType() {
        return DeleteFastFoodEntryCommand.class;
    }

    @Override
    public HttpStatus successStatus() {
        return HttpStatus.NO_CONTENT;
    }

    @Override
    public Object handle(UUID userId, DeleteFastFoodEntryCommand command) {
        entries.delete(userId, command.id());
        return null;
    }
}
