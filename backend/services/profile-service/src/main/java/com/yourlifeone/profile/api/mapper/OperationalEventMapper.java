package com.yourlifeone.profile.api.mapper;

import com.yourlifeone.profile.api.request.OperationalEventRequest;
import com.yourlifeone.profile.domain.OperationalEvent;
import java.util.List;
import org.mapstruct.Mapper;

/** Keeps only the fields that become metrics; event IDs are retained only in the bounded deduplication cache, never metric tags. */
@Mapper
public interface OperationalEventMapper {
    OperationalEvent toDomain(OperationalEventRequest request);

    List<OperationalEvent> toDomain(List<OperationalEventRequest> requests);
}
