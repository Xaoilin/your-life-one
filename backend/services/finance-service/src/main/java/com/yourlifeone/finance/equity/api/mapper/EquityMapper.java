package com.yourlifeone.finance.equity.api.mapper;

import com.yourlifeone.finance.equity.api.response.PositionResponse;
import com.yourlifeone.finance.equity.api.response.PositionsResponse;
import com.yourlifeone.finance.equity.domain.EquityPosition;
import com.yourlifeone.finance.equity.domain.PositionPage;
import org.mapstruct.Mapper;

@Mapper
public interface EquityMapper {
    PositionResponse toResponse(EquityPosition position);

    PositionsResponse toResponse(PositionPage page);
}
