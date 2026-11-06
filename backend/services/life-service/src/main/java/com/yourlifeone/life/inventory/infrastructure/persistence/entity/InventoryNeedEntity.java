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

@Entity
@Table(schema = "inventory", name = "need")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryNeedEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String name;
    private String category;
    private String subcategory;
    private String imageUrl;
    private String linkedItemId;
    private String projectCatalogKey;
    private BigDecimal requiredQuantity;
    private String unit;
    private BigDecimal dimensionLength;
    private BigDecimal dimensionWidth;
    private BigDecimal dimensionHeight;
    private String dimensionUnit;
    private String priority;
    private String status;
    private String notes;
    private Instant orderedAt;
    private Instant acquiredAt;
    private Instant dismissedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** Rows of {@code inventory.need_specification}, in order. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "inventory", name = "need_specification", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "userId"),
            @JoinColumn(name = "need_id", referencedColumnName = "id")})
    @OrderColumn(name = "position")
    @Builder.Default
    private List<SpecificationEmbeddable> specifications = new ArrayList<>();
}
