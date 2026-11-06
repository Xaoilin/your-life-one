package com.yourlifeone.knowledge.lifestyle.application.service;

import com.yourlifeone.knowledge.lifestyle.domain.LifestyleItem;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifestyleColumnsTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-09-01T12:00:00Z");

    private static LifestyleItem item(String id, String type, int sortOrder, Instant createdAt) {
        return LifestyleItem.builder().id(id).type(type).sortOrder(sortOrder).createdAt(createdAt).updatedAt(EARLIER)
                .build();
    }

    private static List<String> ids(List<LifestyleItem> items) {
        return items.stream().map(item -> item.id() + ":" + item.sortOrder()).toList();
    }

    @Test
    void aColumnIsOrderedByPositionThenByWhenItemsWereAdded() {
        var items = List.of(item("b", "haram", 0, NOW), item("a", "haram", 0, EARLIER), item("c", "halal", 0, NOW));

        assertEquals(List.of("a:0", "b:0"), ids(LifestyleColumns.column(items, "haram")));
    }

    @Test
    void insertingRenumbersFromZeroAndClampsThePosition() {
        var column = List.of(item("a", "haram", 0, EARLIER), item("b", "haram", 3, EARLIER));
        var moved = item("m", "haram", 9, EARLIER);

        assertEquals(List.of("m:0", "a:1", "b:2"), ids(LifestyleColumns.insert(column, moved, 0, NOW)));
        assertEquals(List.of("a:0", "b:1", "m:2"), ids(LifestyleColumns.insert(column, moved, 99, NOW)));
        assertEquals(EARLIER, LifestyleColumns.insert(column, moved, null, NOW).getFirst().updatedAt());
    }
}
