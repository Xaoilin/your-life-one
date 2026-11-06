package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.shared.AllowedValues;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Values;
import com.yourlifeone.life.trips.config.TripsProperties;
import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.ItineraryItem;
import com.yourlifeone.life.trips.domain.Trip;
import com.yourlifeone.life.trips.domain.TripLeg;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Trip rules: allowed values, trimmed text, dates in order, and each booking kind's own fields. */
@Component
@RequiredArgsConstructor
public class TripRules {
    private final TripsProperties properties;

    public Trip normalized(Trip trip) {
        var currency = blankToNull(trip.budgetCurrency());
        var normalized = trip.toBuilder()
                .name(trip.name().trim())
                .summary(trip.summary() == null ? "" : trip.summary().trim())
                .notes(trip.notes() == null ? "" : trip.notes())
                .budgetCurrency(currency == null ? properties.defaultCurrency() : currency.toUpperCase(Locale.ROOT))
                .build();
        RecordIds.require(normalized.id());
        AllowedValues.require("status", normalized.status(), properties.statuses());
        if (!normalized.budgetCurrency().matches("[A-Z]{3}")) {
            throw ApiException.badRequest("invalid_currency", "budgetCurrency must be a three-letter currency code.");
        }
        requireOrder(normalized.startDate(), normalized.endDate());
        return normalized;
    }

    public TripLeg normalized(TripLeg leg) {
        var normalized = leg.toBuilder().country(leg.country().trim()).city(leg.city().trim())
                .endDate(Values.orElse(leg.endDate(), leg.startDate())).build();
        RecordIds.require(normalized.id());
        requireOrder(normalized.startDate(), normalized.endDate());
        return normalized;
    }

    public ItineraryItem normalized(ItineraryItem item) {
        var normalized = item.toBuilder().title(item.title().trim()).location(blankToNull(item.location()))
                .startTime(blankToNull(item.startTime())).endTime(blankToNull(item.endTime()))
                .notes(item.notes() == null ? "" : item.notes()).build();
        RecordIds.require(normalized.id());
        return normalized;
    }

    /** Keeps only the fields of the booking's kind, with the app's fallbacks for blank titles. */
    public Booking normalized(Booking booking) {
        var common = booking.toBuilder()
                .legId(blankToNull(booking.legId()))
                .provider(blankToNull(booking.provider()))
                .confirmationCode(blankToNull(booking.confirmationCode()))
                .link(blankToNull(booking.link()))
                .notes(booking.notes() == null ? "" : booking.notes());
        var normalized = Booking.TRANSPORT.equals(booking.kind())
                ? common.title(orDefault(booking.title(), "Transport booking"))
                        .mode(AllowedValues.require("mode", booking.mode(), properties.transportModes()))
                        .fromLabel(trimmedOrEmpty(booking.fromLabel())).toLabel(trimmedOrEmpty(booking.toLabel()))
                        .arriveAt(Values.orElse(booking.arriveAt(), booking.departAt()))
                        .propertyName(null).address(null).city(null).country(null)
                        .checkInDate(null).checkOutDate(null).build()
                : common.title(orDefault(booking.title(), "Stay booking"))
                        .propertyName(orDefault(booking.propertyName(), "Accommodation"))
                        .address(blankToNull(booking.address()))
                        .city(trimmedOrEmpty(booking.city())).country(trimmedOrEmpty(booking.country()))
                        .checkOutDate(Values.orElse(booking.checkOutDate(), booking.checkInDate()))
                        .mode(null).fromLabel(null).toLabel(null).departAt(null).arriveAt(null).build();
        RecordIds.require(normalized.id());
        AllowedValues.optional("budgetStatus", normalized.budgetStatus(), properties.budgetStatuses());
        return normalized;
    }

    public BudgetEntry normalized(BudgetEntry entry) {
        var normalized = entry.toBuilder().title(entry.title().trim())
                .notes(entry.notes() == null ? "" : entry.notes()).build();
        RecordIds.require(normalized.id());
        AllowedValues.require("category", normalized.category(), properties.budgetCategories());
        AllowedValues.require("status", normalized.status(), properties.budgetStatuses());
        return normalized;
    }

    private static void requireOrder(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw ApiException.badRequest("invalid_dates", "The end date must not be before the start date.");
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimmedOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
