package com.yourlifeone.finance.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The banking review ({@code app.finance.review.*}).
 *
 * @param maxDocumentBytes the largest review, as compact JSON
 */
@ConfigurationProperties("app.finance.review")
public record ReviewProperties(int maxDocumentBytes) {
}
