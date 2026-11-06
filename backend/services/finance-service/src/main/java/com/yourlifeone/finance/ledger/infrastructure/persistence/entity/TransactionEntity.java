package com.yourlifeone.finance.ledger.infrastructure.persistence.entity;

import com.yourlifeone.finance.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "finance", name = "transaction")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String type;
    private long amount;
    private String category;
    private String accountId;
    private String toAccountId;
    private String description;
    private LocalDate date;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
