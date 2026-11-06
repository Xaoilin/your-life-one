package com.yourlifeone.calendar.infrastructure.persistence.mapper;

import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.calendar.domain.CalendarEvent;
import com.yourlifeone.calendar.domain.CalendarSource;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.infrastructure.persistence.entity.AccountEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.EventEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.SourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CalendarPersistenceMapper {
    CalendarAccount toAccount(AccountEntity entity);

    CalendarSource toSource(SourceEntity entity);

    @Mapping(target = "timing", expression = "java(toTiming(entity))")
    CalendarEvent toEvent(EventEntity entity);

    default EventTiming toTiming(EventEntity entity) {
        return new EventTiming(entity.isAllDay(), entity.getStartsAt(), entity.getEndsAt());
    }
}
