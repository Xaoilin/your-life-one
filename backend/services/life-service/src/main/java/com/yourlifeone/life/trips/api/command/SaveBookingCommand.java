package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.life.trips.api.request.BookingRequest;

/** Creates or replaces one booking; the queued payload carries the ID from the path. */
public record SaveBookingCommand(String id, BookingRequest booking) {
}
