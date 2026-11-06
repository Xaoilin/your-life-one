package com.yourlifeone.finance.equity.infrastructure.persistence.mapper;

import com.yourlifeone.finance.equity.domain.EquityPosition;
import com.yourlifeone.finance.equity.domain.EquityPositionDraft;
import com.yourlifeone.finance.equity.infrastructure.persistence.entity.PositionEntity;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper
public interface PositionPersistenceMapper {
    default EquityPosition toDomain(PositionEntity entity) {
        return toDomain(entity.getDocument(), entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    EquityPosition toDomain(EquityPositionDraft document, String id, Instant createdAt, Instant updatedAt);
}
