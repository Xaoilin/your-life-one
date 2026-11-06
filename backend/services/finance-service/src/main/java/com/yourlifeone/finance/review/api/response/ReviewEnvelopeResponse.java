package com.yourlifeone.finance.review.api.response;

/** @param review the user's review, or null when there is none yet */
public record ReviewEnvelopeResponse(FinanceReviewResponse review) {
}
