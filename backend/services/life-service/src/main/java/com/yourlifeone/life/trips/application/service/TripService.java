package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import com.yourlifeone.life.trips.config.TripsProperties;
import com.yourlifeone.life.trips.domain.Trip;
import com.yourlifeone.life.trips.domain.TripBundle;
import com.yourlifeone.life.trips.domain.TripLeg;
import com.yourlifeone.life.trips.domain.TripPlan;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.mapper.TripsPersistenceMapper;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripLegRepository;
import com.yourlifeone.life.trips.infrastructure.persistence.repository.TripRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trip writes: a finished plan creates the trip and its parts together; a trip delete removes them all. */
@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository trips;
    private final TripLegRepository legs;
    private final TripPartService parts;
    private final TripsPersistenceMapper persistence;
    private final TripRules rules;
    private final TripsProperties properties;
    private final TripQueries queries;
    private final Clock clock;

    /**
     * Creates the trip, or replaces its own fields keeping its creation time. A trip with dated legs keeps
     * the dates they give it; the requested dates apply only to a trip without them.
     */
    @Transactional
    public TripBundle save(UUID userId, Trip requested) {
        var existing = trips.findById(new UserScopedId(userId, RecordIds.require(requested.id())));
        var now = Timestamps.now(clock);
        var range = TripRoute.range(legs.findByUserIdAndTripIdOrderBySortOrder(userId, requested.id()).stream()
                .map(persistence::toDomain).toList());
        var dated = range.startDate() != null;
        var trip = rules.normalized(requested.toBuilder()
                .startDate(dated ? range.startDate() : requested.startDate())
                .endDate(dated ? range.endDate() : requested.endDate())
                .budgetTotal(Math.max(0, requested.budgetTotal()))
                .createdAt(existing.map(TripEntity::getCreatedAt).orElse(now))
                .updatedAt(now).build());
        trips.save(persistence.toEntity(userId, trip));
        return queries.bundle(userId, trip.id());
    }

    /** Creates a new trip with its route (in the plan's order) and its bookings, dated by its legs. */
    @Transactional
    public TripBundle createPlan(UUID userId, TripPlan plan) {
        if (trips.existsById(new UserScopedId(userId, RecordIds.require(plan.trip().id())))) {
            return queries.bundle(userId, plan.trip().id());
        }
        if (plan.legs().size() > properties.maxLegs()) {
            throw ApiException.badRequest("too_many_legs", "A trip has at most " + properties.maxLegs() + " legs.");
        }
        var legIds = new HashSet<String>();
        plan.legs().forEach(leg -> {
            if (!legIds.add(leg.id())) throw ApiException.badRequest("duplicate_leg", "Leg " + leg.id() + " appears twice.");
        });
        var now = Timestamps.now(clock);
        var range = TripRoute.range(plan.legs().stream().map(rules::normalized).toList());
        var trip = rules.normalized(plan.trip().toBuilder().budgetTotal(Math.max(0, plan.trip().budgetTotal()))
                .startDate(range.startDate()).endDate(range.endDate()).createdAt(now).updatedAt(now).build());
        trips.save(persistence.toEntity(userId, trip));
        for (TripLeg leg : plan.legs()) {
            legs.save(persistence.toEntity(userId, rules.normalized(leg.toBuilder().createdAt(now).updatedAt(now).build())));
        }
        legs.flush();
        plan.bookings().forEach(booking -> parts.saveBooking(userId, booking.id() == null
                ? booking.toBuilder().id(UUID.randomUUID().toString()).build() : booking));
        return queries.bundle(userId, trip.id());
    }

    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!trips.existsById(key)) throw ApiException.notFound("trip_not_found", "This trip does not exist.");
        trips.deleteById(key);
    }
}
