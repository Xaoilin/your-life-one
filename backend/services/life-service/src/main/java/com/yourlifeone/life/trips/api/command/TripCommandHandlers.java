package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.life.trips.api.mapper.TripsMapper;
import com.yourlifeone.life.trips.api.request.TripPlanRequest;
import com.yourlifeone.life.trips.application.service.TripPartService;
import com.yourlifeone.life.trips.application.service.TripService;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * The trip writes the command queue performs. Every write but a trip delete answers with the whole
 * trip, so the app replaces its copy with the stored (and refitted) one.
 */
@Configuration(proxyBeanMethods = false)
public class TripCommandHandlers {
    public static final String CREATE_PLAN = "trips.create-plan";
    public static final String SAVE_TRIP = "trips.save-trip";
    public static final String DELETE_TRIP = "trips.delete-trip";
    public static final String SAVE_LEG = "trips.save-leg";
    public static final String DELETE_LEG = "trips.delete-leg";
    public static final String MOVE_LEG = "trips.move-leg";
    public static final String SAVE_ITINERARY_ITEM = "trips.save-itinerary-item";
    public static final String DELETE_ITINERARY_ITEM = "trips.delete-itinerary-item";
    public static final String SAVE_BOOKING = "trips.save-booking";
    public static final String DELETE_BOOKING = "trips.delete-booking";
    public static final String SAVE_BUDGET_ENTRY = "trips.save-budget-entry";
    public static final String DELETE_BUDGET_ENTRY = "trips.delete-budget-entry";

    @Bean
    WriteCommandHandler<TripPlanRequest> createPlanHandler(TripService trips, TripsMapper mapper) {
        return handler(CREATE_PLAN, TripPlanRequest.class, (user, plan) -> mapper.toResponse(
                trips.createPlan(user, mapper.toDomain(plan))));
    }

    @Bean
    WriteCommandHandler<SaveTripCommand> saveTripHandler(TripService trips, TripsMapper mapper) {
        return handler(SAVE_TRIP, SaveTripCommand.class, (user, command) -> mapper.toResponse(
                trips.save(user, mapper.toDomain(command.id(), command.trip()))));
    }

    @Bean
    WriteCommandHandler<DeleteCommand> deleteTripHandler(TripService trips) {
        return new Handler<>(DELETE_TRIP, DeleteCommand.class, HttpStatus.NO_CONTENT, (user, command) -> {
            trips.delete(user, command.id());
            return null;
        });
    }

    @Bean
    WriteCommandHandler<SaveLegCommand> saveLegHandler(TripPartService parts, TripsMapper mapper) {
        return handler(SAVE_LEG, SaveLegCommand.class, (user, command) -> mapper.toResponse(
                parts.saveLeg(user, mapper.toDomain(command.id(), command.leg()))));
    }

    @Bean
    WriteCommandHandler<DeleteCommand> deleteLegHandler(TripPartService parts, TripsMapper mapper) {
        return handler(DELETE_LEG, DeleteCommand.class, (user, command) -> mapper.toResponse(
                parts.deleteLeg(user, command.id())));
    }

    @Bean
    WriteCommandHandler<MoveLegCommand> moveLegHandler(TripPartService parts, TripsMapper mapper) {
        return handler(MOVE_LEG, MoveLegCommand.class, (user, command) -> mapper.toResponse(
                parts.moveLeg(user, command.id(), command.direction())));
    }

    @Bean
    WriteCommandHandler<SaveItineraryItemCommand> saveItineraryItemHandler(TripPartService parts, TripsMapper mapper) {
        return handler(SAVE_ITINERARY_ITEM, SaveItineraryItemCommand.class, (user, command) -> mapper.toResponse(
                parts.saveItineraryItem(user, mapper.toDomain(command.id(), command.item()))));
    }

    @Bean
    WriteCommandHandler<DeleteCommand> deleteItineraryItemHandler(TripPartService parts, TripsMapper mapper) {
        return handler(DELETE_ITINERARY_ITEM, DeleteCommand.class, (user, command) -> mapper.toResponse(
                parts.deleteItineraryItem(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveBookingCommand> saveBookingHandler(TripPartService parts, TripsMapper mapper) {
        return handler(SAVE_BOOKING, SaveBookingCommand.class, (user, command) -> mapper.toResponse(
                parts.saveBooking(user, mapper.toDomain(command.id(), command.booking()))));
    }

    @Bean
    WriteCommandHandler<DeleteCommand> deleteBookingHandler(TripPartService parts, TripsMapper mapper) {
        return handler(DELETE_BOOKING, DeleteCommand.class, (user, command) -> mapper.toResponse(
                parts.deleteBooking(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveBudgetEntryCommand> saveBudgetEntryHandler(TripPartService parts, TripsMapper mapper) {
        return handler(SAVE_BUDGET_ENTRY, SaveBudgetEntryCommand.class, (user, command) -> mapper.toResponse(
                parts.saveBudgetEntry(user, mapper.toDomain(command.id(), command.entry()))));
    }

    @Bean
    WriteCommandHandler<DeleteCommand> deleteBudgetEntryHandler(TripPartService parts, TripsMapper mapper) {
        return handler(DELETE_BUDGET_ENTRY, DeleteCommand.class, (user, command) -> mapper.toResponse(
                parts.deleteBudgetEntry(user, command.id())));
    }

    private static <P> WriteCommandHandler<P> handler(String type, Class<P> payload, BiFunction<UUID, P, Object> run) {
        return new Handler<>(type, payload, HttpStatus.OK, run);
    }

    /** One command type, its payload and what it does. */
    private record Handler<P>(String type, Class<P> payloadType, HttpStatus successStatus,
            BiFunction<UUID, P, Object> run) implements WriteCommandHandler<P> {
        @Override
        public Object handle(UUID userId, P payload) {
            return run.apply(userId, payload);
        }
    }
}
