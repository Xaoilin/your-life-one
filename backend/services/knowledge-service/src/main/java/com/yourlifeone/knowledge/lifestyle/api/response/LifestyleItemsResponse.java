package com.yourlifeone.knowledge.lifestyle.api.response;

import java.util.List;

/** Every item, by position within its column. */
public record LifestyleItemsResponse(List<LifestyleItemResponse> items) {
}
