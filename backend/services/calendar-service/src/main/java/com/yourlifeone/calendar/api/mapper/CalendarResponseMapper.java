package com.yourlifeone.calendar.api.mapper;

import com.yourlifeone.calendar.api.response.AccountResponse;
import com.yourlifeone.calendar.api.response.AccountSyncResponse;
import com.yourlifeone.calendar.api.response.EventResponse;
import com.yourlifeone.calendar.api.response.SourceResponse;
import com.yourlifeone.calendar.application.model.AccountSyncResult;
import com.yourlifeone.calendar.domain.AccountProvider;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.calendar.domain.CalendarEvent;
import com.yourlifeone.calendar.domain.CalendarSource;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CalendarResponseMapper {
    @Mapping(target = "isPrimary", source = "primary")
    AccountResponse toResponse(CalendarAccount account);

    List<AccountResponse> toAccountResponses(List<CalendarAccount> accounts);

    @Mapping(target = "writable", expression = "java(source.isWritable())")
    SourceResponse toResponse(CalendarSource source);

    List<SourceResponse> toSourceResponses(List<CalendarSource> sources);

    @Mapping(target = "allDay", source = "timing.allDay")
    @Mapping(target = "start", expression = "java(event.timing().startValue())")
    @Mapping(target = "end", expression = "java(event.timing().endValue())")
    EventResponse toResponse(CalendarEvent event);

    List<EventResponse> toEventResponses(List<CalendarEvent> events);

    AccountSyncResponse toResponse(AccountSyncResult result);

    List<AccountSyncResponse> toSyncResponses(List<AccountSyncResult> results);

    default String provider(AccountProvider provider) {
        return provider.value();
    }

    default String authStatus(AuthStatus status) {
        return status.value();
    }
}
