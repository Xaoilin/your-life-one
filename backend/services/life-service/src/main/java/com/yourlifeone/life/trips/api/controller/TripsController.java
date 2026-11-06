package com.yourlifeone.life.trips.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.life.trips.api.command.DeleteCommand;
import com.yourlifeone.life.trips.api.command.MoveLegCommand;
import com.yourlifeone.life.trips.api.command.SaveBookingCommand;
import com.yourlifeone.life.trips.api.command.SaveBudgetEntryCommand;
import com.yourlifeone.life.trips.api.command.SaveItineraryItemCommand;
import com.yourlifeone.life.trips.api.command.SaveLegCommand;
import com.yourlifeone.life.trips.api.command.SaveTripCommand;
import com.yourlifeone.life.trips.api.command.TripCommandHandlers;
import com.yourlifeone.life.trips.api.mapper.TripsMapper;
import com.yourlifeone.life.trips.api.request.BookingRequest;
import com.yourlifeone.life.trips.api.request.BudgetEntryRequest;
import com.yourlifeone.life.trips.api.request.ItineraryItemRequest;
import com.yourlifeone.life.trips.api.request.LegRequest;
import com.yourlifeone.life.trips.api.request.MoveLegRequest;
import com.yourlifeone.life.trips.api.request.TripPlanRequest;
import com.yourlifeone.life.trips.api.request.TripRequest;
import com.yourlifeone.life.trips.api.response.TripBundleResponse;
import com.yourlifeone.life.trips.api.response.TripsResponse;
import com.yourlifeone.life.trips.application.service.TripQueries;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trips and their parts. Each write changes one record (a plan creates one trip with its parts) and
 * answers with the whole trip, including dates refitted to its legs and its budget.
 */
@RestController
@RequestMapping("/api/life/v1/trips")
@RequiredArgsConstructor
public class TripsController {
    private final TripQueries queries;
    private final TripsMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public TripsResponse all(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(queries.all(AuthenticatedUser.id(jwt)));
    }

    @PostMapping("/plans")
    public ResponseEntity<TripBundleResponse> createPlan(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody TripPlanRequest request) {
        return send(jwt, key, TripCommandHandlers.CREATE_PLAN, request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripBundleResponse> saveTrip(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody TripRequest request) {
        return send(jwt, key, TripCommandHandlers.SAVE_TRIP, new SaveTripCommand(id, request));
    }

    /** Deletes the trip with every part of it. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key) {
        return WriteCommandResponses.of(commands.send(TripCommandHandlers.DELETE_TRIP, AuthenticatedUser.id(jwt), key,
                new DeleteCommand(id), Void.class));
    }

    @PutMapping("/legs/{id}")
    public ResponseEntity<TripBundleResponse> saveLeg(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody LegRequest request) {
        return send(jwt, key, TripCommandHandlers.SAVE_LEG, new SaveLegCommand(id, request));
    }

    /** Deletes the leg with its day plans and bookings, and renumbers the rest of the route. */
    @DeleteMapping("/legs/{id}")
    public ResponseEntity<TripBundleResponse> deleteLeg(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key) {
        return send(jwt, key, TripCommandHandlers.DELETE_LEG, new DeleteCommand(id));
    }

    @PostMapping("/legs/{id}/move")
    public ResponseEntity<TripBundleResponse> moveLeg(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody MoveLegRequest request) {
        return send(jwt, key, TripCommandHandlers.MOVE_LEG, new MoveLegCommand(id, request.direction()));
    }

    @PutMapping("/itinerary-items/{id}")
    public ResponseEntity<TripBundleResponse> saveItineraryItem(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id, @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody ItineraryItemRequest request) {
        return send(jwt, key, TripCommandHandlers.SAVE_ITINERARY_ITEM, new SaveItineraryItemCommand(id, request));
    }

    @DeleteMapping("/itinerary-items/{id}")
    public ResponseEntity<TripBundleResponse> deleteItineraryItem(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id, @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key) {
        return send(jwt, key, TripCommandHandlers.DELETE_ITINERARY_ITEM, new DeleteCommand(id));
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<TripBundleResponse> saveBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody BookingRequest request) {
        return send(jwt, key, TripCommandHandlers.SAVE_BOOKING, new SaveBookingCommand(id, request));
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<TripBundleResponse> deleteBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key) {
        return send(jwt, key, TripCommandHandlers.DELETE_BOOKING, new DeleteCommand(id));
    }

    @PutMapping("/budget-entries/{id}")
    public ResponseEntity<TripBundleResponse> saveBudgetEntry(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id, @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key,
            @Valid @RequestBody BudgetEntryRequest request) {
        return send(jwt, key, TripCommandHandlers.SAVE_BUDGET_ENTRY, new SaveBudgetEntryCommand(id, request));
    }

    @DeleteMapping("/budget-entries/{id}")
    public ResponseEntity<TripBundleResponse> deleteBudgetEntry(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id, @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String key) {
        return send(jwt, key, TripCommandHandlers.DELETE_BUDGET_ENTRY, new DeleteCommand(id));
    }

    private ResponseEntity<TripBundleResponse> send(Jwt jwt, String key, String type, Object payload) {
        UUID userId = AuthenticatedUser.id(jwt);
        return WriteCommandResponses.of(commands.send(type, userId, key, payload, TripBundleResponse.class));
    }
}
