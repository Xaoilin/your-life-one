package com.yourlifeone.finance.equity.api.response;

import java.util.List;

/** @param total how many positions match the search, across every page */
public record PositionsResponse(List<PositionResponse> positions, long total) {
}
