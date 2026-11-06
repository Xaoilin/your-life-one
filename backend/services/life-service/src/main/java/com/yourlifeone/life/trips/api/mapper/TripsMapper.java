package com.yourlifeone.life.trips.api.mapper;

import com.yourlifeone.life.trips.api.request.BookingRequest;
import com.yourlifeone.life.trips.api.request.BudgetEntryRequest;
import com.yourlifeone.life.trips.api.request.ItineraryItemRequest;
import com.yourlifeone.life.trips.api.request.LegRequest;
import com.yourlifeone.life.trips.api.request.PlanLegRequest;
import com.yourlifeone.life.trips.api.request.TripPlanRequest;
import com.yourlifeone.life.trips.api.request.TripRequest;
import com.yourlifeone.life.trips.api.response.BudgetSummaryResponse;
import com.yourlifeone.life.trips.api.response.TripBundleResponse;
import com.yourlifeone.life.trips.api.response.TripResponse;
import com.yourlifeone.life.trips.api.response.TripsResponse;
import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.BudgetSummary;
import com.yourlifeone.life.trips.domain.ItineraryItem;
import com.yourlifeone.life.trips.domain.Trip;
import com.yourlifeone.life.trips.domain.TripBundle;
import com.yourlifeone.life.trips.domain.TripLeg;
import com.yourlifeone.life.trips.domain.TripPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TripsMapper {
    @Mapping(target = "id", source = "trip.id")
    @Mapping(target = "createdAt", source = "trip.createdAt")
    @Mapping(target = "updatedAt", source = "trip.updatedAt")
    TripResponse toResponse(Trip trip, BudgetSummary budget);

    BudgetSummaryResponse toResponse(BudgetSummary budget);

    @Mapping(target = "trip", expression = "java(toResponse(bundle.trip(), bundle.budget()))")
    TripBundleResponse toResponse(TripBundle bundle);

    /** Every trip's bundle flattened into one response, trips in the given order. */
    default TripsResponse toResponse(List<TripBundle> bundles) {
        var trips = bundles.stream().map(bundle -> toResponse(bundle.trip(), bundle.budget())).toList();
        var responses = bundles.stream().map(this::toResponse).toList();
        return new TripsResponse(trips,
                responses.stream().flatMap(bundle -> bundle.legs().stream()).toList(),
                responses.stream().flatMap(bundle -> bundle.itineraryItems().stream()).toList(),
                responses.stream().flatMap(bundle -> bundle.bookings().stream()).toList(),
                responses.stream().flatMap(bundle -> bundle.budgetEntries().stream()).toList());
    }

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Trip toDomain(String id, TripRequest request);

    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TripLeg toDomain(String id, LegRequest request);

    @Mapping(target = "sortOrder", expression = "java(request.sortOrder() == null ? 0 : request.sortOrder())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ItineraryItem toDomain(String id, ItineraryItemRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toDomain(String id, BookingRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BudgetEntry toDomain(String id, BudgetEntryRequest request);

    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TripLeg toDomain(PlanLegRequest request);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toDomain(BookingRequest request);

    /** Legs take their route order from their place in the plan. */
    default TripPlan toDomain(TripPlanRequest request) {
        var legs = new ArrayList<TripLeg>();
        IntStream.range(0, request.legs().size()).forEach(index -> legs.add(
                toDomain(request.legs().get(index)).toBuilder().tripId(request.id()).sortOrder(index).build()));
        var bookings = request.bookings().stream()
                .map(booking -> toDomain(booking).toBuilder().tripId(request.id()).build()).toList();
        return new TripPlan(toDomain(request.id(), request.trip()), legs, bookings);
    }
}
