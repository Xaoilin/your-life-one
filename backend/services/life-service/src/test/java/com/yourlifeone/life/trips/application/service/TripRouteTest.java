package com.yourlifeone.life.trips.application.service;

import com.yourlifeone.life.trips.domain.TripLeg;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TripRouteTest {
    private static TripLeg leg(String id, int order, String start, String end) {
        return TripLeg.builder().id(id).tripId("t").city(id).country("c").sortOrder(order)
                .startDate(start == null ? null : LocalDate.parse(start))
                .endDate(end == null ? null : LocalDate.parse(end)).build();
    }

    @Test
    void theRangeRunsFromTheFirstDatedLegToTheLastInRouteOrder() {
        var range = TripRoute.range(List.of(leg("b", 1, "2026-07-02", "2026-07-15"), leg("x", 2, null, null),
                leg("a", 0, "2026-06-17", "2026-07-02")));

        assertEquals(LocalDate.parse("2026-06-17"), range.startDate());
        assertEquals(LocalDate.parse("2026-07-15"), range.endDate());
    }

    @Test
    void noDatedLegGivesNoRange() {
        var range = TripRoute.range(List.of(leg("x", 0, null, null)));

        assertNull(range.startDate());
        assertNull(range.endDate());
    }

    @Test
    void movingSwapsNeighboursAndDoesNothingAtTheEnds() {
        var legs = List.of(leg("a", 0, null, null), leg("b", 1, null, null), leg("c", 2, null, null));

        var moved = TripRoute.move(legs, "b", -1);
        assertEquals(List.of("b", "a", "c"), moved.stream().sorted(TripRoute.ORDER).map(TripLeg::id).toList());
        assertEquals(List.of(0, 1, 2), TripRoute.move(legs, "a", -1).stream().map(TripLeg::sortOrder).toList());
    }

    @Test
    void renumberingClosesGapsInRouteOrder() {
        var renumbered = TripRoute.renumbered(List.of(leg("c", 5, null, null), leg("a", 1, null, null)));

        assertEquals(List.of("a", "c"), renumbered.stream().map(TripLeg::id).toList());
        assertEquals(List.of(0, 1), renumbered.stream().map(TripLeg::sortOrder).toList());
    }
}
