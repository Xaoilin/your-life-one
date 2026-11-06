package com.yourlifeone.finance.review.infrastructure.persistence.entity;

import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** The user's one banking review: the whole document, with the columns the service queries. */
@Entity
@Table(schema = "finance", name = "review")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {
    @Id
    private UUID userId;
    @JdbcTypeCode(SqlTypes.JSON)
    private FinanceReviewDraft document;
    private LocalDate asOf;
    private Instant createdAt;
    private Instant updatedAt;
}
