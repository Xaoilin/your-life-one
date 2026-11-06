package com.yourlifeone.life.inventory.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.life.inventory.api.command.AcquireNeedCommand;
import com.yourlifeone.life.inventory.api.command.AcquireNeedHandler;
import com.yourlifeone.life.inventory.api.command.ArchiveItemCommand;
import com.yourlifeone.life.inventory.api.command.ArchiveItemHandler;
import com.yourlifeone.life.inventory.api.command.SaveItemCommand;
import com.yourlifeone.life.inventory.api.command.SaveItemHandler;
import com.yourlifeone.life.inventory.api.command.SaveItemsHandler;
import com.yourlifeone.life.inventory.api.command.SaveNeedCommand;
import com.yourlifeone.life.inventory.api.command.SaveNeedHandler;
import com.yourlifeone.life.inventory.api.mapper.InventoryMapper;
import com.yourlifeone.life.inventory.api.request.AcquireNeedRequest;
import com.yourlifeone.life.inventory.api.request.ItemRequest;
import com.yourlifeone.life.inventory.api.request.NeedRequest;
import com.yourlifeone.life.inventory.api.request.SaveItemsRequest;
import com.yourlifeone.life.inventory.api.response.AcquisitionResponse;
import com.yourlifeone.life.inventory.api.response.InventoryItemResponse;
import com.yourlifeone.life.inventory.api.response.InventoryNeedResponse;
import com.yourlifeone.life.inventory.api.response.InventoryResponse;
import com.yourlifeone.life.inventory.api.response.SavedItemsResponse;
import com.yourlifeone.life.inventory.api.response.StockCheckResponse;
import com.yourlifeone.life.inventory.application.service.InventoryQueries;
import com.yourlifeone.life.inventory.domain.InventorySearch;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory: the authoritative stock. The app reads everything; agents search and check stock. There
 * is no delete: items are archived and needs are acquired or dismissed, one record per write.
 */
@RestController
@RequestMapping("/api/life/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryQueries queries;
    private final InventoryMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public InventoryResponse all(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(queries.all(AuthenticatedUser.id(jwt)));
    }

    @GetMapping("/search")
    public InventoryResponse search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) String projectCatalogKey,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "50") int limit) {
        var search = InventorySearch.builder().query(query).projectCatalogKey(projectCatalogKey).category(category)
                .location(location).limit(limit).build();
        return mapper.toResponse(queries.search(AuthenticatedUser.id(jwt), search));
    }

    @GetMapping("/check")
    public StockCheckResponse check(@AuthenticationPrincipal Jwt jwt, @RequestParam String name,
            @RequestParam(defaultValue = "1") BigDecimal requiredQuantity,
            @RequestParam(required = false) String unit) {
        return mapper.toResponse(queries.check(AuthenticatedUser.id(jwt), name, requiredQuantity, unit));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponse> saveItem(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ItemRequest request) {
        return WriteCommandResponses.of(commands.send(SaveItemHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveItemCommand(id, request), InventoryItemResponse.class));
    }

    @PostMapping("/items")
    public ResponseEntity<SavedItemsResponse> saveItems(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SaveItemsRequest request) {
        return WriteCommandResponses.of(commands.send(SaveItemsHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, SavedItemsResponse.class));
    }

    @PostMapping("/items/{id}/archive")
    public ResponseEntity<InventoryItemResponse> archiveItem(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(ArchiveItemHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new ArchiveItemCommand(id), InventoryItemResponse.class));
    }

    @PutMapping("/needs/{id}")
    public ResponseEntity<InventoryNeedResponse> saveNeed(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody NeedRequest request) {
        return WriteCommandResponses.of(commands.send(SaveNeedHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveNeedCommand(id, request), InventoryNeedResponse.class));
    }

    @PostMapping("/needs/{id}/acquire")
    public ResponseEntity<AcquisitionResponse> acquireNeed(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) AcquireNeedRequest request) {
        var newItemId = request == null ? null : request.newItemId();
        return WriteCommandResponses.of(commands.send(AcquireNeedHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new AcquireNeedCommand(id, newItemId), AcquisitionResponse.class));
    }
}
