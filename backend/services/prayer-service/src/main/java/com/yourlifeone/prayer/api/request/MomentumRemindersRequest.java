package com.yourlifeone.prayer.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The pillars whose momentum reminder preferences changed; the others keep theirs. */
public record MomentumRemindersRequest(@NotNull @Size(min = 1, max = 2) List<@Valid @NotNull MomentumReminderRequest> pillars) {
}
