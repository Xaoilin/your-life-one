package com.yourlifeone.finance.review.infrastructure.persistence.mapper;

import com.yourlifeone.finance.review.domain.FinanceReview;
import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import com.yourlifeone.finance.review.infrastructure.persistence.entity.ReviewEntity;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper
public interface ReviewPersistenceMapper {
    default FinanceReview toDomain(ReviewEntity entity) {
        return toDomain(entity.getDocument(), FinanceReview.CURRENT, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    FinanceReview toDomain(FinanceReviewDraft document, String id, Instant createdAt, Instant updatedAt);
}
