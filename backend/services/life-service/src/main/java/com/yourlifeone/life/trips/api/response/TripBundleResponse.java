package com.yourlifeone.life.trips.api.response;

import java.util.List;

/** One trip after a write: the trip (with its budget) and every part, so the app replaces its copy whole. */
public record TripBundleResponse(TripResponse trip, List<TripLegResponse> legs,
        List<ItineraryItemResponse> itineraryItems, List<BookingResponse> bookings,
        List<BudgetEntryResponse> budgetEntries) {
}
