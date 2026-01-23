# Azkary - Islamic Remembrance App

An Android application for daily Islamic remembrances (Azkar) with progress tracking, multi-language support, and a beautiful modern UI.

## 📱 Features

### Core Functionality

#### 🕌 Azkar Categories
- **Morning Adhkar** (أذكار الصباح) - 25 remembrances
- **Evening Adhkar** (أذكار المساء) - 22 remembrances  
- **Sleep Adhkar** (أذكار النوم) - 16 remembrances
- Each category contains authentic Islamic supplications with references

#### 📖 Reading Experience
- **Full-screen reading interface** with swipeable pages
- **Arabic text** with proper RTL support
- **Transliteration** for non-Arabic speakers
- **English translations** for understanding
- **Hadith references** with source citations
- **Tap-to-increment counter** for repetition tracking
- **Auto-advance** to next dhikr when completed
- **Haptic feedback** on interactions
- **Progress bar** showing weighted completion

#### 📊 Progress Tracking
- **Daily progress monitoring** for each category
- **Weighted progress calculation** based on text length and repetitions
- **Circular progress indicators** on summary cards
- **Persistent storage** of user progress per date
- **Current session** highlighting on home screen
- **Completion status** for individual items

#### 🌍 Multi-Language Support
- **System language detection** (Arabic/English)
- **Manual language selection** via Settings
- **RTL/LTR layout** automatic switching
- **Localized content** for all categories and items
- **Bidirectional text** rendering support

#### 🎨 UI/UX Features
- **Material Design 3** theming
- **Gradient cards** for current session
- **Smooth animations** for progress changes
- **Dark theme** with custom navy color scheme
- **Beautiful typography** optimized for Arabic text
- **Responsive layouts** for different screen sizes
- **Modern card-based** design

### Technical Features

#### 💾 Data Management
- **Room Database** for local storage
- **Seeded database** with authentic azkar content
- **JSON schema validation** for data integrity
- **Version-controlled seed data** (Schema v4)
- **Foreign key relationships** for data consistency
- **Indexed queries** for performance

#### 🏗️ Architecture
- **MVVM pattern** with ViewModel and Repository
- **Dependency Injection** with Hilt/Dagger
- **Reactive streams** with Kotlin Flow
- **Coroutines** for async operations
- **Jetpack Compose** for modern UI
- **Navigation Component** for screen transitions

#### 📱 User Preferences
- **DataStore** for persistent settings
- **Language preference** storage
- **User progress** saved per date
- **Category customization** support

## 🛠️ Tech Stack

### Android Core
- **Language**: Kotlin
- **Min SDK**: 33 (Android 13)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java**: Version 17

### Libraries & Frameworks
- **UI**: Jetpack Compose with Material 3
- **Database**: Room 2.6+
- **DI**: Hilt/Dagger 2.51+
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **Storage**: DataStore Preferences
- **Serialization**: Kotlinx Serialization JSON
- **Build**: Gradle with KTS, KSP

### Key Dependencies

### WIP Features
Epic: Prayer-Time–Based Azkar Scheduling
Task 1 — Add permissions + location provider

Goal: Obtain user location reliably (or fallback).

Add permissions:

android.permission.ACCESS_COARSE_LOCATION

(Optional) android.permission.ACCESS_FINE_LOCATION if you truly need it

Implement LocationRepository using Fused Location Provider:

suspend fun getCurrentLocation(): Location?

Cache last known location locally

Handle cases:

Permission denied → return null

Location services off → return null

Acceptance

App can request permission from Settings screen

App can read location when granted

Safe fallback when denied/unavailable

Task 2 — Settings UI: Location controls

Goal: Settings screen controls location mode + manual location.

Create a Settings section:

Toggle: Use location for prayer times

Row: Current location (lat/long or reverse-geocoded city if available)

Button: Refresh location

Manual mode:

City/Country input (or a simple “Manual location” form)

Save manual location coordinates if you support geocoding

Persist using DataStore:

useLocation: Boolean

manualLocation: LatLng? (or city/country if you’re not geocoding)

lastResolvedLocation: LatLng?

Acceptance

User can switch between location and manual mode

Values persist across restarts

Task 3 — Networking: Aladhan API client

Goal: Retrofit client + DTOs.

Add Retrofit + OkHttp + Moshi/Gson

Create AladhanApi:

GET /v1/calendar/{dateURI}

GET /v1/methods (optional, can be hardcoded if you pick one)

DTOs for:

Daily timings: Fajr, Asr, Isha (and timezone/date fields if needed)

Acceptance

Can fetch calendar for a given month + lat/long

Parses prayer timings successfully

Task 4 — Local storage: Monthly prayer times cache (Room)

Goal: Store 1 month of prayer times per location + method.

Room entities:

PrayerMonthEntity

id

year, month

lat, lng

methodId (or method name)

fetchedAtEpochMillis

PrayerDayEntity

monthId

dateEpochDay (or yyyy-MM-dd)

fajrMillis, asrMillis, ishaMillis (store as epoch millis in local tz)

(Optional) raw strings for debugging

DAO:

getMonth(year, month, lat, lng, methodId): PrayerMonthWithDays?

upsertMonth(month + days)

deleteOldMonths(keepLastN = 2) (optional)

Acceptance

After one fetch, app works offline for that month

Reads month + days fast

Task 5 — Repository: Fetch-once-per-month logic

Goal: Central orchestration.

Implement PrayerTimesRepository:

suspend fun ensureMonthCached(date: LocalDate, location: LatLng, methodId: Int): PrayerMonthWithDays

If cached month exists for same year/month and near-same location → return cached

Else fetch from API and persist

Location matching strategy (simple + practical):

Treat location “same” if within ~5–10 km OR round lat/lng to 2 decimals

Acceptance

API is called at most once per month per location/method

If location changes meaningfully → fetch again

Task 6 — Time parsing + timezone correctness

Goal: Convert API times safely into epoch millis.

Create PrayerTimeParser:

Input: "05:12 (EET)" style strings (Aladhan sometimes includes timezone suffix)

Output: epoch millis for that date in device timezone (or API timezone if provided)

Implementation notes:

Strip non-time suffixes

Use LocalDate + LocalTime → ZonedDateTime

Acceptance

Times are correct across daylight saving / timezone changes

No crashes on unexpected formats

Task 7 — Azkar state engine (current zikr)

Goal: Determine which azkar is active “right now”.

Create:

enum class AzkarWindow { MORNING, NIGHT, SLEEP }
data class AzkarSchedule(val window: AzkarWindow, val start: Instant, val end: Instant)


Function:

fun currentWindow(now: Instant, today: PrayerDay, tomorrow: PrayerDay): AzkarSchedule

Rules:

Morning: Fajr → Asr

Night: Asr → Isha

Sleep: Isha → next day Fajr (跨 midnight)

Acceptance

Correct window shown at any time of day

Correct around midnight and before fajr

Task 8 — Scheduling notifications (optional if app already schedules)

Goal: Update notification schedule daily from cached prayer times.

If you use WorkManager/AlarmManager:

Create a daily worker AzkarScheduleWorker that:

Loads today+tomorrow prayer times from cache

Schedules three window notifications or updates ongoing reminder

Ensure it runs after midnight and after month refresh

Acceptance

Notifications reflect prayer windows daily

No repeated/duplicated alarms

Task 9 — App startup hook

Goal: Ensure cache exists and engine is ready.

On app start (or first open of Azkar screen):

Determine active location:

If useLocation → fetch location (with fallback to last known)

Else use manual

Call ensureMonthCached(LocalDate.now(), location, methodId)

Update current window state (ViewModel)

Acceptance

First open works even with no cache (fetches once)

Subsequent opens use cache instantly

Task 10 — Failure handling + UX

Goal: Robustness.

If API fails:

If cached exists → use cached + show “Using last saved prayer times”

Else → show error with retry

If permission denied:

Show manual location UI prompt

Acceptance

No hard failure paths

User always has a usable fallback

Suggested Kotlin Module Structure

data/remote/AladhanApi.kt

data/local/room/*

data/repo/PrayerTimesRepository.kt

domain/AzkarWindowEngine.kt

ui/settings/*