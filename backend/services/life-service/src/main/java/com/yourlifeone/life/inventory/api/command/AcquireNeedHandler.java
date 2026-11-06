package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.application.service.InventoryNeedService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Acquires a need from the command queue: the need closes and its quantity joins the stock together. */
@Component
@RequiredArgsConstructor
public class AcquireNeedHandler implements WriteCommandHandler<AcquireNeedCommand> {
    public static final String TYPE = "inventory.acquire-need";

    private final InventoryNeedService needs;
    private final InventoryMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<AcquireNeedCommand> payloadType() {
        return AcquireNeedCommand.class;
    }

    @Override
    public Object handle(UUID userId, AcquireNeedCommand command) {
        return mapper.toResponse(needs.acquire(userId, command.needId(), command.newItemId()));
    }
}
