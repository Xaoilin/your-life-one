package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.api.request.SaveItemsRequest;
import com.yourlifeone.life.inventory.api.response.SavedItemsResponse;
import com.yourlifeone.life.inventory.application.service.InventoryItemService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Creates or enriches a batch of items from the command queue, all or none. */
@Component
@RequiredArgsConstructor
public class SaveItemsHandler implements WriteCommandHandler<SaveItemsRequest> {
    public static final String TYPE = "inventory.save-items";

    private final InventoryItemService items;
    private final InventoryMapper mapper;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<SaveItemsRequest> payloadType() {
        return SaveItemsRequest.class;
    }

    @Override
    public Object handle(UUID userId, SaveItemsRequest request) {
        return new SavedItemsResponse(mapper.toItemResponses(items.saveAll(userId, mapper.toCandidates(request.items()))));
    }
}
