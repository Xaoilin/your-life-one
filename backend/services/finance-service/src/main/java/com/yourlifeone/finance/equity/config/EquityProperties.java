package com.yourlifeone.finance.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Equity positions ({@code app.finance.equity.*}).
 *
 * @param maxPositions the most positions one user keeps
 * @param maxQueryLength the longest company search
 * @param maxDocumentBytes the largest position, as compact JSON
 */
@ConfigurationProperties("app.finance.equity")
public record EquityProperties(int maxPositions, int defaultPageSize, int maxPageSize, int maxOffset,
        int maxQueryLength, int maxDocumentBytes) {
}
