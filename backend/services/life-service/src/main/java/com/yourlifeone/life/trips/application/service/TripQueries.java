package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.domain.TripBundle;
import com.yourlifeone.life.trips.infrastructure.persistence.mapper.TripsPersistenceMapper;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.BookingRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.BudgetEntryRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.ItineraryItemRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripLegRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads trips as bundles: each trip with its parts and its budget. */
@Service
@RequiredArgsConstructor
public class TripQueries {
    private final TripRepository trips;
    private final TripLegRepository legs;
    private final ItineraryItemRepository itineraryItems;
    private final BookingRepository bookings;
    private final BudgetEntryRepository budgetEntries;
    private final TripsPersistenceMapper persistence;
    private final TripBudgetCalculator budget;

    /** Every trip, newest first. */
    @Transactional(readOnly = true)
    public List<TripBundle> all(UUID userId) {
        return trips.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(trip -> bundle(userId, trip.getId())).toList();
    }

    @Transactional(readOnly = true)
    public TripBundle bundle(UUID userId, String tripId) {
        var trip = trips.findById(new UserScopedId(userId, RecordIds.require(tripId))).map(persistence::toDomain)
                .orElseThrow(() -> ApiException.notFound("trip_not_found", "This trip does not exist."));
        var tripLegs = legs.findByUserIdAndTripIdOrderBySortOrder(userId, tripId).stream()
                .map(persistence::toDomain).sorted(TripRoute.ORDER).toList();
        var items = itineraryItems.findByUserIdAndTripIdOrderByDateAscSortOrderAsc(userId, tripId).stream()
                .map(persistence::toDomain).toList();
        var tripBookings = bookings.findByUserIdAndTripIdOrderByCreatedAt(userId, tripId).stream()
                .map(persistence::toDomain).toList();
        var entries = budgetEntries.findByUserIdAndTripIdOrderByCreatedAt(userId, tripId).stream()
                .map(persistence::toDomain).toList();
        return new TripBundle(trip, budget.summarize(trip, tripBookings, entries), tripLegs, items, tripBookings,
                entries);
    }
}
