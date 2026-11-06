package com.yourlifeone.prayer.infrastructure.theshia;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.infrastructure.theshia.dto.GeocodingLocation;
import com.yourlifeone.prayer.infrastructure.theshia.dto.GeocodingResponse;
import org.springframework.web.client.RestClient;

/** Open-Meteo/GeoNames resolves cities outside TheShia's curated city list. */
public class PrayerLocationResolver {
    private final RestClient geocoding;

    public PrayerLocationResolver(RestClient geocoding) {
        this.geocoding = geocoding;
    }

    public GeocodingLocation resolve(String city, String country) {
        var response = geocoding.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("name", "{location}")
                        .queryParam("count", 10)
                        .queryParam("language", "en")
                        .queryParam("format", "json")
                        .build(city + ", " + country))
                .retrieve().body(GeocodingResponse.class);
        if (response == null) throw new InvalidTimetableException("missing geocoding response");
        if (response.results() == null) throw locationNotFound();
        return response.results().stream()
                .filter(location -> location.featureCode() != null && location.featureCode().startsWith("PPL"))
                .findFirst().orElseThrow(PrayerLocationResolver::locationNotFound);
    }

    private static ApiException locationNotFound() {
        return ApiException.badRequest("location_not_found", "No prayer times were found for this city and country.");
    }
}
