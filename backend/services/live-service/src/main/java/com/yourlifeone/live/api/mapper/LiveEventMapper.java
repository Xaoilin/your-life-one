package com.yourlifeone.live.api.mapper;

import com.yourlifeone.commonservice.changes.ChangeEvent;
import com.yourlifeone.live.api.response.LiveEventResponse;
import org.mapstruct.Mapper;

/** What a tab receives of a change event: everything but whose it is. */
@Mapper
public interface LiveEventMapper {
    LiveEventResponse toResponse(ChangeEvent event);
}
