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

### Task 3: Networking — Aladhan API

**Goal:** Fetch prayer times from network.

**Tech**
* Retrofit + OkHttp (Add dependencies)
* **Kotlin Serialization** (Matches project stack)

**Endpoints**
* `/v1/calendar/{year}/{month}`
* `/v1/methods` (optional)

**DTOs**
* Daily timings (Fajr, Asr, Isha)
* Timezone metadata

**Acceptance**
* Can fetch a full month
* Parses timings correctly

---

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
