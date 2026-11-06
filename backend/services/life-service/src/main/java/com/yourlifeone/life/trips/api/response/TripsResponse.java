package com.yourlifeone.life.trips.api.response;

import java.util.List;

/** Every trip (each with its budget) and every part of them, newest trip first. */
public record TripsResponse(List<TripResponse> trips, List<TripLegResponse> legs,
        List<ItineraryItemResponse> itineraryItems, List<BookingResponse> bookings,
        List<BudgetEntryResponse> budgetEntries) {
}
