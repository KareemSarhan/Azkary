# Azkary — Development Plan

## Architecture Notes

### MVVM Compliance
The project follows MVVM correctly across all 4 screens (Summary, Reading, CategoryCreation, Settings).
Layer separation is clean: `Composables → ViewModels → Repositories → DAOs/DataStore`.

**Nuance — Raw `Flow` vs `StateFlow` in ViewModels**

Some ViewModels expose raw `Flow` instead of `StateFlow`:

```kotlin
// ReadingViewModel.kt — raw Flow
val items: Flow<List<AzkarItemUi>> = localeManager.currentLangTagFlow.flatMapLatest { ... }
val weightedProgress: Flow<Float> = localeManager.currentLangTagFlow.flatMapLatest { ... }
```

This works, but `StateFlow` is preferable for UI-facing state because:
- It caches the last value — no re-fetch on recomposition
- Composables don't need to provide an `initial` value in `collectAsState(initial = ...)`
- Consistent contract: one source of truth, always has a value

**Fix:** Convert raw `Flow` properties to `StateFlow` using `.stateIn()`:

```kotlin
val items: StateFlow<List<AzkarItemUi>> = localeManager.currentLangTagFlow
    .flatMapLatest { lang -> repository.observeItemsForCategory(...) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

Affected files:
- `ui/reading/ReadingViewModel.kt` — `items`, `weightedProgress`
- `ui/summary/SummaryViewModel.kt` — `categories`
- `ui/category/CategoryCreationViewModel.kt` — `availableItems` (already uses `.stateIn()` here, good)

---

## Bug Fixes

### Notifications Not Working

The notification system has several issues that together prevent notifications from ever firing.

---

#### 1. Missing POST_NOTIFICATIONS Permission Check (Critical — Android 13+)
**File:** `app/src/main/java/com/app/azkary/notification/AzkarNotificationManager.kt`

On Android 13+ (API 33+), calling `notificationManager.notify()` without checking the
`POST_NOTIFICATIONS` permission causes a `SecurityException`. A `NotificationPermissionHelper`
is already injected into `AzkarNotificationManager` but never called.

**Fix:** Guard `notify()` with a permission check before posting.

---

#### 2. Notifications Never Scheduled on First Launch (Critical)
**File:** `app/src/main/java/com/app/azkary/AzkaryApp.kt`

Notification scheduling only happens in two places:
- `NotificationBootReceiver` — only runs after a device reboot
- `SettingsViewModel` — only runs when the user manually refreshes prayer times

On a fresh install, notifications are never scheduled until the user explicitly triggers it.

**Fix:** In `AzkaryApp.onCreate()`, check if location is set and if any category has
`notificationEnabled = true`, then trigger scheduling.

---

#### 3. All Categories Default to `notificationEnabled = false` (Critical)
**File:** `app/src/main/java/com/app/azkary/data/local/entities/AzkarEntities.kt` (line 18)

The DAO query (`getCategoriesWithNotifications`) filters by `notificationEnabled = 1`.
Since all seeded categories default to `false`, the notification worker finds zero categories
and sends nothing — even if everything else works.

**Fix:** Either default the 3 system categories (Morning, Evening, Sleep) to
`notificationEnabled = true` in `SeedManager.kt`, or prompt the user during onboarding.

---

#### 4. Broken `.first()` Extension in NotificationBootReceiver (Critical)
**File:** `app/src/main/java/com/app/azkary/notification/NotificationBootReceiver.kt` (lines 66–73)

A custom `.first()` extension is defined that doesn't actually stop collection:

```kotlin
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect {
        result = it
        return@collect  // does NOT stop collection
    }
    return result!!  // NPE if flow emits nothing
}
```

This causes the boot receiver to either hang (collecting forever) or crash with a NPE,
meaning notifications are never rescheduled after a device reboot.

**Fix:** Delete the custom extension and use `kotlinx.coroutines.flow.first()` from stdlib.

---

### Fix Priority

| # | Issue | File | Severity |
|---|-------|------|----------|
| 1 | All categories default to `notificationEnabled=false` | `entities/AzkarEntities.kt`, `seed/SeedManager.kt` | Critical |
| 2 | No scheduling on first launch | `AzkaryApp.kt` | Critical |
| 3 | Broken `.first()` in boot receiver | `NotificationBootReceiver.kt` | Critical |
| 4 | Missing permission check before `notify()` | `AzkarNotificationManager.kt` | Critical (Android 13+) |
| 5 | Convert raw `Flow` to `StateFlow` in ViewModels | `ReadingViewModel.kt`, `SummaryViewModel.kt` | Minor |
