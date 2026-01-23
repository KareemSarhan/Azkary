# WIP — Prayer-Time–Based Azkar Scheduling

## 🎯 Epic Goal
Automatically determine and schedule the correct Azkar window
(Morning / Night / Sleep) based on **real prayer times**, with full offline support.

---

## Phase 1 — Location & Settings

### Task 1: Location Permissions & Provider [DONE]
**Goal:** Obtain user location reliably with safe fallbacks.

**Implementation ✅**
- Permissions:
  - `android.permission.ACCESS_COARSE_LOCATION`
  - `android.permission.ACCESS_FINE_LOCATION`
- `LocationRepository` implemented using `FusedLocationProviderClient`.
- DI configured with Hilt (`AppModule` & `RepositoryModule`).
- Logic handles permission checks and uses `lastLocation` as a fallback before requesting fresh coordinates.

**Acceptance ✅**
- App requests permission (to be handled in UI).
- Location is read when granted.
- Safe null handling when unavailable.

---

### Task 2: Settings UI — Location Controls

**Goal:** Let user control location-based prayer times.

**Implementation ✅**

**UI Components:**
* Toggle: `Use location for prayer times` with iOS-style animated switch
* Location display row showing:
    - **City name** as primary text (e.g., "Riyadh, SA")
    - GPS coordinates as secondary detail
    - Refresh button with loading indicator
* Permission request flow integrated
* Snackbar notifications for errors
* Smooth expand/collapse animations

**Architecture:**
* `LocationRepository` - GPS location via FusedLocationProviderClient
* `GeocodingRepository` - Reverse geocoding using Android Geocoder API
    - Converts coordinates → city names
    - Graceful fallback to coordinates if geocoding fails
    - Runs on IO dispatcher for performance
* `SettingsViewModel` - State management with:
    - `toggleUseLocation()` - enable/disable with auto-refresh
    - `refreshLocation()` - fetch GPS + reverse geocode
    - Loading states and error handling
* `SettingsScreen` - Polished UI with:
    - Organized sections (PRAYER TIMES / GENERAL)
    - Navy theme styling consistency
    - Accessibility considerations

**DataStore Persistence:**
kotlin data class LocationPreferences

**Files Added/Modified:**
* `data/repository/GeocodingRepository.kt` (new)
* `data/model/LatLng.kt` (new)
* `data/prefs/UserPreferencesRepository.kt` (updated)
* `ui/settings/SettingsViewModel.kt` (updated)
* `ui/settings/SettingsScreen.kt` (updated)
* `di/AppModule.kt` (updated with GeocodingRepository binding)

**Acceptance ✅**
* Toggle works instantly
* Values persist across app restarts
* Location refresh with runtime permission checks
* **City name displayed** (e.g., "Dubai, AE") instead of raw coordinates
* Coordinates shown as fallback when geocoding unavailable
* Clean error messages via Snackbar
* Loading indicator during location fetch
* Smooth UI animations

---

## Phase 2 — Prayer Times Data

# Task 3: Networking — Aladhan API (Offline-First) (MVP)

## Goal
Fetch and persist real prayer times for a **Gregorian month** using the Aladhan API, with reliable parsing, graceful error handling, and **full offline support after first sync**.

---

## Scope

### 1. API Integration (MVP Only)

**Base URL**  
https://api.aladhan.com/v1

**Endpoint**  
GET /calendar/{year}/{month}

#### Path Parameters
- year: Integer (Gregorian year)
- month: Integer (1–12)

#### Query Parameters (Only These)
- latitude: Double (required)
- longitude: Double (required)
- method: Integer (default: 4 – Umm Al-Qura, Makkah)
- school: Integer (default: 0 – Shafi)

_No optional parameters are supported in this phase._

#### Returns
this is a trimmed down version of the full API response
showing only one day.
~~~json
{
  "code": 200,
  "status": "OK",
  "data": [
    {
      "timings": {
        "Fajr": "06:06 (UTC)",
        "Sunrise": "08:11 (UTC)",
        "Dhuhr": "12:11 (UTC)",
        "Asr": "13:54 (UTC)",
        "Sunset": "16:03 (UTC)",
        "Maghrib": "16:02 (UTC)",
        "Isha": "18:07 (UTC)",
        "Imsak": "05:58 (UTC)",
        "Midnight": "23:58 (UTC)",
        "Firstthird": "21:24 (UTC)",
        "Lastthird": "02:45 (UTC)"
      },
      "date": {
        "readable": "01 Jan 2025",
        "timestamp": "1735722061",
        "gregorian": {
          "date": "01-01-2025",
          "format": "DD-MM-YYYY",
          "day": "01",
          "weekday": {
            "en": "Wednesday"
          },
          "month": {
            "number": 1,
            "en": "January"
          },
          "year": "2025",
          "designation": {
            "abbreviated": "AD",
            "expanded": "Anno Domini"
          },
          "lunarSighting": false
        },
        "hijri": {
          "date": "01-07-1446",
          "format": "DD-MM-YYYY",
          "day": "1",
          "weekday": {
            "en": "Al Arba'a",
            "ar": "الاربعاء"
          },
          "month": {
            "number": 7,
            "en": "Rajab",
            "ar": "رَجَب",
            "days": 30
          },
          "year": "1446",
          "designation": {
            "abbreviated": "AH",
            "expanded": "Anno Hegirae"
          },
          "holidays": [
            "Beginning of the holy months"
          ],
          "adjustedHolidays": [],
          "method": "UAQ"
        }
      },
      "meta": {
        "latitude": 51.5194682,
        "longitude": -0.1360365,
        "timezone": "UTC",
        "method": {
          "id": 3,
          "name": "Muslim World League",
          "params": {
            "Fajr": 18,
            "Isha": 17
          },
          "location": {
            "latitude": 51.5194682,
            "longitude": -0.1360365
          }
        },
        "latitudeAdjustmentMethod": "ANGLE_BASED",
        "midnightMode": "STANDARD",
        "school": "STANDARD",
        "offset": {
          "Imsak": "5",
          "Fajr": "3",
          "Sunrise": "5",
          "Dhuhr": "7",
          "Asr": "9",
          "Maghrib": "-1",
          "Sunset": 0,
          "Isha": "8",
          "Midnight": "-6"
        }
      }
    }
  ]
}
~~~
---

## Data Models (DTOs)

Create Kotlin Serialization DTOs with **lenient parsing**.

### PrayerCalendarResponseDto
- code: Int
- status: String
- data: List<PrayerDayDto>

### PrayerDayDto
- timings: PrayerTimingsDto
- date: DateDto
- meta: MetaDto

### PrayerTimingsDto
(Only required prayers)
- Fajr: String
- Dhuhr: String
- Asr: String
- Maghrib: String
- Isha: String

### DateDto
- gregorian: GregorianDateDto

### GregorianDateDto
- date: String
- month: MonthDto

### MonthDto
- number: Int

### MetaDto
- timezone: String

**JSON Configuration**
- ignoreUnknownKeys = true

---

## Timing Parsing Rule

Prayer times are returned as strings and may include suffixes such as:
- `06:06 (UTC)`
- `05:12 (+03)`

Parsing rules:
1. Extract the first `HH:mm` value from the string
2. Convert to `LocalTime` in the domain layer
3. If parsing fails, throw `ParsingException`

---

## Networking Stack

### Tech
- Retrofit
- OkHttp
- Kotlin Serialization
- Coroutines (suspend APIs)

### OkHttp Configuration
- Reasonable connect/read/write timeouts
- Logging interceptor enabled **only in debug builds**

### Manifest
- INTERNET permission required

---

## Repository Design (Offline-First)

### PrayerTimesRepository API
- `getMonthlyCalendar(year, month, latitude, longitude, method = 4, school = 0)`

### Data Flow
1. Load cached month (if available) and return immediately
2. If network is available:
   - Fetch data from Aladhan API
   - Persist the full month locally (Room recommended)
   - Return updated data
3. If network is unavailable:
   - Return cached data
   - If no cache exists, throw `NoCachedDataException`

### Cache Key
- year
- month
- rounded latitude / longitude (2–3 decimals)
- method
- school

---

## Error Handling (MVP)

Domain-level exceptions:
- `NetworkException` — connectivity, timeout, DNS failures
- `ApiException` — non-2xx HTTP responses
- `ParsingException` — serialization or time parsing failures
- `NoCachedDataException` — offline with no cached data

---

## Dependency Injection

Provide via Hilt:
- AladhanService (Retrofit interface)
- OkHttpClient and interceptors
- Retrofit instance with Kotlin Serialization converter
- PrayerTimesRepository
- Cache datasource (Room DAO + entity)
- Network availability monitor

---

## Acceptance Criteria

- Fetches a full monthly prayer calendar for a given location
- Correctly parses Fajr, Dhuhr, Asr, Maghrib, and Isha
- Normalizes timing strings into `LocalTime`
- Exposes timezone from API meta
- Persists monthly data locally
- Works fully offline after first successful sync
- Gracefully handles network, API, and parsing errors
- Uses suspend-based Retrofit APIs
- Enables HTTP logging only in debug builds
```

### Task 4: Local Cache — Room

**Goal:** Offline-first monthly cache.

**Entities**
* `PrayerMonthEntity`
* `PrayerDayEntity`

**DAO**
```kotlin
getMonth(year, month, lat, lng, methodId)
upsertMonth(...)
deleteOldMonths(keepLastN = 2)
```

**Acceptance**
* Works fully offline after first fetch
* Fast reads

---

### Task 5: Repository Logic

**Goal:** Fetch once per month per location.

**Rules**
* Reuse cache if:
    * Same year/month
    * Location within ~5–10 km (or rounded coords)

**Acceptance**
* Network called at most once/month
* Location change triggers refetch

---

## Phase 3 — Time & Scheduling Logic

### Task 6: Time Parsing & Timezone Safety

**Goal:** Convert API strings → epoch millis safely.

**Notes**
* Strip suffixes like `(EET)`
* Use `java.time` (LocalDate + LocalTime)
* Handle DST correctly

**Acceptance**
* Correct times across DST
* No crashes on format changes

---

### Task 7: Azkar Window Engine

**Goal:** Determine active Azkar window.

**Model**
```kotlin
enum class AzkarWindow { MORNING, NIGHT, SLEEP }

data class AzkarSchedule(
  val window: AzkarWindow,
  val start: Instant,
  val end: Instant
)
```

**Rules**
* Morning: Fajr → Asr
* Night: Asr → Isha
* Sleep: Isha → next day Fajr

**Acceptance**
* Correct around midnight
* Correct before Fajr

---

## Phase 4 — Notifications & App Lifecycle

### Task 8: Notification Scheduling

**Goal:** Update reminders daily.

**Approach**
* WorkManager (Recommended for offline-first)
* Daily worker:
    * Loads today + tomorrow
    * Schedules window notifications

**Acceptance**
* No duplicate alarms
* Updates after midnight

---

### Task 9: App Startup Hook

**Goal:** Ensure data readiness.

**Flow**
1. Resolve location
2. Ensure month cache
3. Compute current window
4. Update ViewModel state

**Acceptance**
* First launch works
* Subsequent launches are instant

---

### Task 10: Failure Handling & UX

**Goal:** No dead ends.

**Scenarios**
* API fails → use cache + warning
* No cache → show retry UI
* Permission denied → manual location prompt

**Acceptance**
* App always usable
* Clear user feedback
