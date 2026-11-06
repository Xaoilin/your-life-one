package com.yourlifeone.life.inventory.api.mapper;

import com.yourlifeone.life.inventory.api.request.ItemCandidateRequest;
import com.yourlifeone.life.inventory.api.request.ItemRequest;
import com.yourlifeone.life.inventory.api.request.NeedRequest;
import com.yourlifeone.life.inventory.api.response.AcquisitionResponse;
import com.yourlifeone.life.inventory.api.response.InventoryItemResponse;
import com.yourlifeone.life.inventory.api.response.InventoryNeedResponse;
import com.yourlifeone.life.inventory.api.response.InventoryResponse;
import com.yourlifeone.life.inventory.api.response.StockCheckResponse;
import com.yourlifeone.life.inventory.domain.Acquisition;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.inventory.domain.InventorySnapshot;
import com.yourlifeone.life.inventory.domain.ItemCandidate;
import com.yourlifeone.life.inventory.domain.StockCheck;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InventoryMapper {
    InventoryItemResponse toResponse(InventoryItem item);

    List<InventoryItemResponse> toItemResponses(List<InventoryItem> items);

    InventoryNeedResponse toResponse(InventoryNeed need);

    InventoryResponse toResponse(InventorySnapshot snapshot);

    StockCheckResponse toResponse(StockCheck check);

    AcquisitionResponse toResponse(Acquisition acquisition);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryItem toDomain(String id, ItemRequest request);

    @Mapping(target = "orderedAt", ignore = true)
    @Mapping(target = "acquiredAt", ignore = true)
    @Mapping(target = "dismissedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryNeed toDomain(String id, NeedRequest request);

    ItemCandidate toCandidate(ItemCandidateRequest request);

    List<ItemCandidate> toCandidates(List<ItemCandidateRequest> requests);
}
