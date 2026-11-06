package com.yourlifeone.finance.review.api.mapper;

import com.yourlifeone.finance.review.api.response.FinanceReviewResponse;
import com.yourlifeone.finance.review.api.response.ReviewEnvelopeResponse;
import com.yourlifeone.finance.review.domain.FinanceReview;
import java.util.Optional;
import org.mapstruct.Mapper;

@Mapper
public interface ReviewMapper {
    FinanceReviewResponse toResponse(FinanceReview review);

    default ReviewEnvelopeResponse toEnvelope(Optional<FinanceReview> review) {
        return new ReviewEnvelopeResponse(review.map(this::toResponse).orElse(null));
    }

    default ReviewEnvelopeResponse toEnvelope(FinanceReview review) {
        return new ReviewEnvelopeResponse(toResponse(review));
    }
}
