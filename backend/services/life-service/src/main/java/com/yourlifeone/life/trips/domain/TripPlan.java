package com.yourlifeone.life.trips.domain;

import java.util.List;

/** A finished trip plan: the trip, its route in order, and bookings that may name those legs. */
public record TripPlan(Trip trip, List<TripLeg> legs, List<Booking> bookings) {
}
