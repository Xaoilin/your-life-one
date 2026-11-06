package com.yourlifeone.life.trips.domain;

import java.util.List;

/** A trip with every part, and its budget worked out from them. */
public record TripBundle(Trip trip, BudgetSummary budget, List<TripLeg> legs, List<ItineraryItem> itineraryItems,
        List<Booking> bookings, List<BudgetEntry> budgetEntries) {
}
