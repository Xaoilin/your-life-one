package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.life.trips.domain.TripLeg;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Route rules: legs in order, a trip's dates from its legs, moving a leg and closing the gap a removed
 * leg leaves. Pure functions, so they are tested without a database.
 */
public final class TripRoute {
    /** Route order: position, then start date (undated last), then city. */
    public static final Comparator<TripLeg> ORDER = Comparator.comparingInt(TripLeg::sortOrder)
            .thenComparing(TripLeg::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TripLeg::city);

    private TripRoute() {
    }

    /** The trip's dates: the first dated leg's start and the last dated leg's end; null when no leg is dated. */
    public static DateRange range(List<TripLeg> legs) {
        var dated = legs.stream().filter(leg -> leg.startDate() != null && leg.endDate() != null).sorted(ORDER).toList();
        if (dated.isEmpty()) return new DateRange(null, null);
        return new DateRange(dated.getFirst().startDate(), dated.getLast().endDate());
    }

    /** The legs with the given one and its neighbour swapped; unchanged at either end of the route. */
    public static List<TripLeg> move(List<TripLeg> legs, String legId, int direction) {
        var ordered = legs.stream().sorted(ORDER).toList();
        var index = IntStream.range(0, ordered.size()).filter(i -> ordered.get(i).id().equals(legId)).findFirst().orElse(-1);
        var target = index + direction;
        if (index < 0 || target < 0 || target >= ordered.size()) return ordered;
        var moved = new ArrayList<>(ordered);
        var leg = ordered.get(index);
        var neighbour = ordered.get(target);
        moved.set(index, neighbour.toBuilder().sortOrder(leg.sortOrder()).build());
        moved.set(target, leg.toBuilder().sortOrder(neighbour.sortOrder()).build());
        return moved;
    }

    /** The legs renumbered 0..n-1 in route order. */
    public static List<TripLeg> renumbered(List<TripLeg> legs) {
        var ordered = legs.stream().sorted(ORDER).toList();
        return IntStream.range(0, ordered.size())
                .mapToObj(index -> ordered.get(index).toBuilder().sortOrder(index).build()).toList();
    }

    /** A trip's start and end; both null for a trip with no dated leg. */
    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
