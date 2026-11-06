package com.yourlifeone.finance.equity.domain;

import java.util.List;

/** One page of the positions that match a search, and how many match in all. */
public record PositionPage(List<EquityPosition> positions, long total) {
}
