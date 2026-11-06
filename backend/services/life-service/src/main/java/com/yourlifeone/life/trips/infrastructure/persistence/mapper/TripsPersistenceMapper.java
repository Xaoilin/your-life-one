package com.yourlifeone.life.trips.infrastructure.persistence.mapper;

import com.yourlifeone.life.trips.domain.Booking;
import com.yourlifeone.life.trips.domain.BudgetEntry;
import com.yourlifeone.life.trips.domain.ItineraryItem;
import com.yourlifeone.life.trips.domain.Trip;
import com.yourlifeone.life.trips.domain.TripLeg;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BookingEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.BudgetEntryEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.ItineraryItemEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripEntity;
import com.yourlifeone.life.trips.infrastructure.persistence.entity.TripLegEntity;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper
public interface TripsPersistenceMapper {
    Trip toDomain(TripEntity entity);

    TripLeg toDomain(TripLegEntity entity);

    ItineraryItem toDomain(ItineraryItemEntity entity);

    Booking toDomain(BookingEntity entity);

    BudgetEntry toDomain(BudgetEntryEntity entity);

    TripEntity toEntity(UUID userId, Trip trip);

    TripLegEntity toEntity(UUID userId, TripLeg leg);

    ItineraryItemEntity toEntity(UUID userId, ItineraryItem item);

    BookingEntity toEntity(UUID userId, Booking booking);

    BudgetEntryEntity toEntity(UUID userId, BudgetEntry entry);
}
