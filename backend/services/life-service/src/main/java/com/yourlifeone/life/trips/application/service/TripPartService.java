package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.config.TripsProperties;
import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.ItineraryItem;
import com.yourlifeone.life.trips.domain.TripBundle;
import com.yourlifeone.life.trips.domain.TripLeg;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BookingEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BudgetEntryEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.ItineraryItemEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripLegEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.mapper.TripsPersistenceMapper;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.BookingRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.BudgetEntryRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.ItineraryItemRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripLegRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes to the parts of a trip. A part stays with its trip (a save naming another trip is refused).
 * Any change to the route refits the trip's dates to its legs; removing a leg removes its day plans
 * and bookings (by foreign key) and renumbers the rest.
 */
@Service
@RequiredArgsConstructor
public class TripPartService {
    private final TripRepository trips;
    private final TripLegRepository legs;
    private final ItineraryItemRepository itineraryItems;
    private final BookingRepository bookings;
    private final BudgetEntryRepository budgetEntries;
    private final TripsPersistenceMapper persistence;
    private final TripRules rules;
    private final TripsProperties properties;
    private final TripQueries queries;
    private final Clock clock;

    @Transactional
    public TripBundle saveLeg(UUID userId, TripLeg requested) {
        var existing = legs.findById(key(userId, requested.id()));
        requireSameTrip(existing.map(TripLegEntity::getTripId), requested.tripId());
        var tripLegs = legs.findByUserIdAndTripIdOrderBySortOrder(userId, requireTrip(userId, requested.tripId()));
        if (existing.isEmpty() && tripLegs.size() >= properties.maxLegs()) {
            throw ApiException.badRequest("too_many_legs", "A trip has at most " + properties.maxLegs() + " legs.");
        }
        var now = Timestamps.now(clock);
        var sortOrder = existing.map(TripLegEntity::getSortOrder)
                .orElseGet(() -> tripLegs.stream().mapToInt(TripLegEntity::getSortOrder).max().orElse(-1) + 1);
        var leg = rules.normalized(requested.toBuilder().sortOrder(sortOrder)
                .createdAt(existing.map(TripLegEntity::getCreatedAt).orElse(now)).updatedAt(now).build());
        legs.saveAndFlush(persistence.toEntity(userId, leg));
        return refitted(userId, leg.tripId(), now);
    }

    @Transactional
    public TripBundle deleteLeg(UUID userId, String id) {
        var leg = legs.findById(key(userId, id))
                .orElseThrow(() -> ApiException.notFound("leg_not_found", "This leg does not exist."));
        legs.delete(leg);
        legs.flush();
        var remaining = legs.findByUserIdAndTripIdOrderBySortOrder(userId, leg.getTripId()).stream()
                .map(persistence::toDomain).toList();
        saveOrder(userId, TripRoute.renumbered(remaining));
        return refitted(userId, leg.getTripId(), Timestamps.now(clock));
    }

    @Transactional
    public TripBundle moveLeg(UUID userId, String id, int direction) {
        var leg = legs.findById(key(userId, id))
                .orElseThrow(() -> ApiException.notFound("leg_not_found", "This leg does not exist."));
        var tripLegs = legs.findByUserIdAndTripIdOrderBySortOrder(userId, leg.getTripId()).stream()
                .map(persistence::toDomain).toList();
        saveOrder(userId, TripRoute.move(tripLegs, id, direction));
        return refitted(userId, leg.getTripId(), Timestamps.now(clock));
    }

    @Transactional
    public TripBundle saveItineraryItem(UUID userId, ItineraryItem requested) {
        var existing = itineraryItems.findById(key(userId, requested.id()));
        requireSameTrip(existing.map(ItineraryItemEntity::getTripId), requested.tripId());
        requireLegOfTrip(userId, requested.legId(), requireTrip(userId, requested.tripId()));
        var now = Timestamps.now(clock);
        var item = rules.normalized(requested.toBuilder()
                .createdAt(existing.map(ItineraryItemEntity::getCreatedAt).orElse(now)).updatedAt(now).build());
        itineraryItems.save(persistence.toEntity(userId, item));
        return queries.bundle(userId, item.tripId());
    }

    @Transactional
    public TripBundle deleteItineraryItem(UUID userId, String id) {
        return deleteOne(userId, id, itineraryItems::findById, ItineraryItemEntity::getTripId, itineraryItems::delete,
                "itinerary_item_not_found");
    }

    @Transactional
    public TripBundle saveBooking(UUID userId, Booking requested) {
        var existing = bookings.findById(key(userId, requested.id()));
        requireSameTrip(existing.map(BookingEntity::getTripId), requested.tripId());
        var tripId = requireTrip(userId, requested.tripId());
        if (requested.legId() != null && !requested.legId().isBlank()) requireLegOfTrip(userId, requested.legId(), tripId);
        var now = Timestamps.now(clock);
        var booking = rules.normalized(requested.toBuilder()
                .createdAt(existing.map(BookingEntity::getCreatedAt).orElse(now)).updatedAt(now).build());
        bookings.save(persistence.toEntity(userId, booking));
        return queries.bundle(userId, tripId);
    }

    @Transactional
    public TripBundle deleteBooking(UUID userId, String id) {
        return deleteOne(userId, id, bookings::findById, BookingEntity::getTripId, bookings::delete,
                "booking_not_found");
    }

    @Transactional
    public TripBundle saveBudgetEntry(UUID userId, BudgetEntry requested) {
        var existing = budgetEntries.findById(key(userId, requested.id()));
        requireSameTrip(existing.map(BudgetEntryEntity::getTripId), requested.tripId());
        var tripId = requireTrip(userId, requested.tripId());
        var now = Timestamps.now(clock);
        var entry = rules.normalized(requested.toBuilder()
                .createdAt(existing.map(BudgetEntryEntity::getCreatedAt).orElse(now)).updatedAt(now).build());
        budgetEntries.save(persistence.toEntity(userId, entry));
        return queries.bundle(userId, tripId);
    }

    @Transactional
    public TripBundle deleteBudgetEntry(UUID userId, String id) {
        return deleteOne(userId, id, budgetEntries::findById, BudgetEntryEntity::getTripId, budgetEntries::delete,
                "budget_entry_not_found");
    }

    private <E> TripBundle deleteOne(UUID userId, String id, Function<UserScopedId, Optional<E>> find,
            Function<E, String> tripOf, Consumer<E> delete, String notFound) {
        var entity = find.apply(key(userId, id))
                .orElseThrow(() -> ApiException.notFound(notFound, "This part of the trip does not exist."));
        delete.accept(entity);
        return queries.bundle(userId, tripOf.apply(entity));
    }

    /** Writes new positions in one flush; the deferred unique position check runs at commit. */
    private void saveOrder(UUID userId, List<TripLeg> ordered) {
        var now = Timestamps.now(clock);
        ordered.forEach(leg -> legs.findById(key(userId, leg.id())).ifPresent(entity -> {
            if (entity.getSortOrder() != leg.sortOrder()) {
                entity.setSortOrder(leg.sortOrder());
                entity.setUpdatedAt(now);
            }
        }));
        legs.flush();
    }

    /** Sets the trip's dates from its legs and answers with the whole trip. */
    private TripBundle refitted(UUID userId, String tripId, Instant now) {
        var trip = trips.findById(key(userId, tripId)).orElseThrow();
        var range = TripRoute.range(legs.findByUserIdAndTripIdOrderBySortOrder(userId, tripId).stream()
                .map(persistence::toDomain).toList());
        trip.setStartDate(range.startDate());
        trip.setEndDate(range.endDate());
        trip.setUpdatedAt(now);
        trips.saveAndFlush(trip);
        return queries.bundle(userId, tripId);
    }

    private String requireTrip(UUID userId, String tripId) {
        if (!trips.existsById(key(userId, tripId))) {
            throw ApiException.notFound("trip_not_found", "This trip does not exist.");
        }
        return tripId;
    }

    private void requireLegOfTrip(UUID userId, String legId, String tripId) {
        var leg = legs.findById(key(userId, legId))
                .orElseThrow(() -> ApiException.notFound("leg_not_found", "This leg does not exist."));
        if (!leg.getTripId().equals(tripId)) {
            throw ApiException.badRequest("leg_of_another_trip", "The leg belongs to another trip.");
        }
    }

    private static void requireSameTrip(Optional<String> storedTripId, String requestedTripId) {
        if (storedTripId.isPresent() && !storedTripId.get().equals(requestedTripId)) {
            throw ApiException.badRequest("trip_changed", "A part of a trip cannot move to another trip.");
        }
    }

    private static UserScopedId key(UUID userId, String id) {
        return new UserScopedId(userId, RecordIds.require(id));
    }
}
