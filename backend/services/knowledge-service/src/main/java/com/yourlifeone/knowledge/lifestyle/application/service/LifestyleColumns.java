package com.yourlifeone.knowledge.lifestyle.application.service;

import com.yourlifeone.knowledge.lifestyle.domain.LifestyleItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Positions within a tracker column: items are shown by position, then by when they were added. Every
 * change renumbers the column from 0, so positions stay gap-free and unique.
 */
public final class LifestyleColumns {
    private static final Comparator<LifestyleItem> COLUMN_ORDER = Comparator
            .comparing(LifestyleItem::sortOrder)
            .thenComparing(LifestyleItem::createdAt)
            .thenComparing(LifestyleItem::id);

    private LifestyleColumns() {
    }

    /** The items of one column, in order. */
    public static List<LifestyleItem> column(List<LifestyleItem> items, String type) {
        return items.stream().filter(item -> item.type().equals(type)).sorted(COLUMN_ORDER).toList();
    }

    /**
     * The column with {@code moved} inserted at {@code position} (the end when null or past it), renumbered.
     * {@code column} must not contain the moved item.
     */
    public static List<LifestyleItem> insert(List<LifestyleItem> column, LifestyleItem moved, Integer position,
            Instant now) {
        var ordered = new ArrayList<>(column);
        var index = position == null ? ordered.size() : Math.clamp(position, 0, ordered.size());
        ordered.add(index, moved);
        return renumber(ordered, now);
    }

    /** Positions 0, 1, 2… in list order; an item whose position changes is stamped {@code now}. */
    public static List<LifestyleItem> renumber(List<LifestyleItem> ordered, Instant now) {
        var renumbered = new ArrayList<LifestyleItem>(ordered.size());
        for (int position = 0; position < ordered.size(); position++) {
            var item = ordered.get(position);
            renumbered.add(item.sortOrder() != null && item.sortOrder() == position
                    ? item : item.toBuilder().sortOrder(position).updatedAt(now).build());
        }
        return renumbered;
    }
}
