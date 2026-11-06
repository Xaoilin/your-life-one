# Direct prayer timetable provider

The replacement uses [TheShia](https://theshia.org/en/api/prayer-times/) directly
from the Spring prayer service, with explicit `method=Jafari`: Fajr16°, Maghrib4°,
Isha14° and its TwilightAngle high-latitude rule. No key, Supabase relay or user
credential is sent. [Open-Meteo geocoding](https://open-meteo.com/en/docs/geocoding-api),
based on GeoNames, resolves the country-qualified city into coordinates and an IANA
timezone; this retains towns such as Bedford absent from TheShia's curated cities.
These are private, non-commercial applications. Cache each validated location/date
in the existing PostgreSQL timetable store. Unknown locations remain400; unavailable,
malformed, wrong-date or wrong-method timetables remain503. No default city fallback.

## Compatibility checked 6 November 2026

Recorded public responses compare matching rounded coordinates, Ja'fari angles and
timezones for London on15January,29March(DST start),21June,25October(DST end),6November;
and Bedford, Basra and Dubai on6November. Fixtures live under the prayer service's
`src/test/resources/theshia/`; tests exercise those exact responses.

| Timing | TheShia minus AlAdhan, eight samples |
| --- | --- |
| Fajr, sunrise, Dhuhr, sunset, Maghrib, Isha | Within one minute |
| Asr | -4 to +3 minutes; use the selected provider's calculation, no invented offset |
| Provider midnight | 27–45 minutes earlier; deliberately not imported |
| Application midnight after normalization | Matches all eight previous values |

The application retains the effective AlAdhan STANDARD convention: midpoint of
sunset and the same day's sunrise clock time advanced24hours, truncating seconds.
This preserves the Isha deadline rule, including local-clock behavior on DST dates.
TheShia's human documentation and OpenAPI disagree about its Fajr-based midnight;
observed responses differ from the old rule. Do not silently adopt that field.
Small differences in rounded sunrise/sunset may still shift normalized midnight by
one minute at other dates/locations. This comparison is integration compatibility,
not a religious ruling or proof that all locations/dates produce identical times.

TheShia's `moonsighting` Hijri display is used when available. On6November it shows
25Jumada al-Awwal1448, whereas AlAdhan's HJCoSA calendar shows26; there is no hidden
one-day offset. A display-date failure does not discard a valid timetable.

Existing cached schedules and user-day references are never overwritten. Historical
records keep the timetable originally used. New uncached dates use the new provider.
The public method0 identifier remains the app's compatible Ja'fari calculation ID.

Two repeated requests for geocoding, timings and Hijri date from the production
Java21 prayer container on6November all returned200 (31–540ms), using normal JSON
Accept and identifying User-Agent headers. Full Spring/release verification and
resource retirement are tracked separately in `docs/supabase-retirement.md`.
