package com.yourlifeone.life.inventory.infrastructure.persistence.mapper;

import com.yourlifeone.life.inventory.domain.Dimensions;
import com.yourlifeone.life.inventory.domain.InventoryItem;
import com.yourlifeone.life.inventory.domain.InventoryNeed;
import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.yourlifeone.life.inventory.infrastructure.persistence.entity.InventoryNeedEntity;
import com.yourlifeone.life.inventory.infrastructure.persistence.entity.SpecificationEmbeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Entities keep dimensions as four columns and specifications as ordered rows. */
@Mapper
public interface InventoryPersistenceMapper {
    @Mapping(target = "dimensions", expression = "java(dimensions(entity.getDimensionLength(), "
            + "entity.getDimensionWidth(), entity.getDimensionHeight(), entity.getDimensionUnit()))")
    InventoryItem toDomain(InventoryItemEntity entity);

    @Mapping(target = "dimensions", expression = "java(dimensions(entity.getDimensionLength(), "
            + "entity.getDimensionWidth(), entity.getDimensionHeight(), entity.getDimensionUnit()))")
    InventoryNeed toDomain(InventoryNeedEntity entity);

    @Mapping(target = "dimensionLength", source = "item.dimensions.length")
    @Mapping(target = "dimensionWidth", source = "item.dimensions.width")
    @Mapping(target = "dimensionHeight", source = "item.dimensions.height")
    @Mapping(target = "dimensionUnit", source = "item.dimensions.unit")
    InventoryItemEntity toEntity(UUID userId, InventoryItem item);

    @Mapping(target = "dimensionLength", source = "need.dimensions.length")
    @Mapping(target = "dimensionWidth", source = "need.dimensions.width")
    @Mapping(target = "dimensionHeight", source = "need.dimensions.height")
    @Mapping(target = "dimensionUnit", source = "need.dimensions.unit")
    InventoryNeedEntity toEntity(UUID userId, InventoryNeed need);

    /** Copies every field but the owner and ID, replacing specifications in place. */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specifications", ignore = true)
    @Mapping(target = "dimensionLength", source = "dimensions.length")
    @Mapping(target = "dimensionWidth", source = "dimensions.width")
    @Mapping(target = "dimensionHeight", source = "dimensions.height")
    @Mapping(target = "dimensionUnit", source = "dimensions.unit")
    void copy(InventoryItem item, @MappingTarget InventoryItemEntity entity);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specifications", ignore = true)
    @Mapping(target = "dimensionLength", source = "dimensions.length")
    @Mapping(target = "dimensionWidth", source = "dimensions.width")
    @Mapping(target = "dimensionHeight", source = "dimensions.height")
    @Mapping(target = "dimensionUnit", source = "dimensions.unit")
    void copy(InventoryNeed need, @MappingTarget InventoryNeedEntity entity);

    default void update(InventoryItemEntity entity, InventoryItem item) {
        copy(item, entity);
        if (item.dimensions() == null) clearDimensions(entity);
        entity.getSpecifications().clear();
        entity.getSpecifications().addAll(toRows(item.specifications()));
    }

    default void update(InventoryNeedEntity entity, InventoryNeed need) {
        copy(need, entity);
        if (need.dimensions() == null) clearDimensions(entity);
        entity.getSpecifications().clear();
        entity.getSpecifications().addAll(toRows(need.specifications()));
    }

    default Dimensions dimensions(BigDecimal length, BigDecimal width, BigDecimal height, String unit) {
        return unit == null ? null : new Dimensions(length, width, height, unit);
    }

    default Map<String, String> toMap(List<SpecificationEmbeddable> rows) {
        var map = new LinkedHashMap<String, String>();
        if (rows != null) rows.forEach(row -> map.put(row.getName(), row.getValue()));
        return map;
    }

    default List<SpecificationEmbeddable> toRows(Map<String, String> specifications) {
        var rows = new ArrayList<SpecificationEmbeddable>();
        if (specifications != null) specifications.forEach((name, value) -> rows.add(new SpecificationEmbeddable(name, value)));
        return rows;
    }

    private static void clearDimensions(InventoryItemEntity entity) {
        entity.setDimensionLength(null);
        entity.setDimensionWidth(null);
        entity.setDimensionHeight(null);
        entity.setDimensionUnit(null);
    }

    private static void clearDimensions(InventoryNeedEntity entity) {
        entity.setDimensionLength(null);
        entity.setDimensionWidth(null);
        entity.setDimensionHeight(null);
        entity.setDimensionUnit(null);
    }
}
