package com.yourlifeone.life.inventory.infrastructure.persistence.entity;

import com.yourlifeone.life.shared.UserScopedId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
@Table(schema = "inventory", name = "item")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String name;
    private String category;
    private String subcategory;
    private String imageUrl;
    private String trackingMode;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal lowStockThreshold;
    private String brand;
    private String model;
    private BigDecimal dimensionLength;
    private BigDecimal dimensionWidth;
    private BigDecimal dimensionHeight;
    private String dimensionUnit;
    private String condition;
    private String location;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;
    private String notes;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> projectCatalogKeys;
    private Instant lastVerifiedAt;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** Rows of {@code inventory.item_specification}, in order. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "inventory", name = "item_specification", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "userId"),
            @JoinColumn(name = "item_id", referencedColumnName = "id")})
    @OrderColumn(name = "position")
    @Builder.Default
    private List<SpecificationEmbeddable> specifications = new ArrayList<>();
}
