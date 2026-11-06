package com.yourlifeone.knowledge.lifestyle.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Where to move an item: a column and a position in it.
 *
 * @param position 0 for the top; absent for the end
 */
public record MoveItemRequest(@NotBlank @Size(max = 32) String type, @Min(0) Integer position) {
}
