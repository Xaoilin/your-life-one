package com.yourlifeone.finance.equity.infrastructure.persistence.entity;

import com.yourlifeone.finance.equity.domain.EquityPositionDraft;
import com.yourlifeone.finance.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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

/** One equity position: the whole document, with the columns the service searches and sorts by. */
@Entity
@Table(schema = "equity", name = "position")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    @JdbcTypeCode(SqlTypes.JSON)
    private EquityPositionDraft document;
    private String company;
    private LocalDate asOf;
    private Instant createdAt;
    private Instant updatedAt;
}
