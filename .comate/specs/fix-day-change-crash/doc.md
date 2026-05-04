# Fix: Day-Change Crash on Reading Page

## Problem

When the user leaves the app on a zikr reading page and the day changes (e.g., overnight), reopening the app crashes. The root cause is that `todayFlow` in `ReadingViewModel` is a one-shot cold flow that emits the Islamic date once and never re-emits. When the day changes and the process is killed by Android:

1. The ViewModel is recreated with a new `todayFlow` emitting the **new** date
2. `items` StateFlow starts with `initialValue = emptyList()`
3. `HorizontalPager` has `currentPage` restored from saved state to a value > 0, but `pageCount` is 0
4. The pager crashes with `IllegalArgumentException` / `IndexOutOfBoundsException`

Even without process death, there's a **read/write date mismatch**: reads use the stale `todayFlow` date, but writes (`incrementRepeat`, `markItemComplete`) call `islamicDateProvider.getCurrentDate()` fresh, so user taps appear to do nothing.

The same one-shot flow pattern exists in `SummaryViewModel`.

## Architecture and Technical Approach

### 1. Replace one-shot `todayFlow` with a reactive date flow

Convert `todayFlow` from a cold one-shot flow to a reactive `StateFlow<String>` that:
- Emits the current Islamic date on creation
- Re-emits when the day changes (detected via lifecycle or periodic check)
- Is shared across reads AND writes to eliminate the date mismatch

### 2. Add day-change detection on app resume

When `MainActivity.onResume()` is called, check if the Islamic date has changed since the last known date. If it has, trigger a refresh so ViewModels pick up the new date.

### 3. Ensure pager safety during empty-item transitions

Guard the `HorizontalPager` against the case where `items` is temporarily empty during date transitions, preventing out-of-bounds access.

## Affected Files

| File | Modification Type | Details |
|------|------------------|---------|
| `app/src/main/java/com/app/azkary/domain/IslamicDateProvider.kt` | Modify | Add a reactive `SharedFlow`/`StateFlow` that emits the date and updates on demand |
| `app/src/main/java/com/app/azkary/ui/reading/ReadingViewModel.kt` | Modify | Replace `todayFlow` with subscription to `IslamicDateProvider`'s reactive date flow; use the same date source for reads and writes |
| `app/src/main/java/com/app/azkary/ui/summary/SummaryViewModel.kt` | Modify | Same pattern: replace one-shot flow with reactive date |
| `app/src/main/java/com/app/azkary/MainActivity.kt` | Modify | Call `islamicDateProvider.refreshDate()` in `onResume()` to detect day changes |
| `app/src/main/java/com/app/azkary/ui/reading/ReadingScreen.kt` | Modify | Add safety for empty items list during transitions |

## Implementation Details

### IslamicDateProvider - Add reactive date StateFlow

```kotlin
@Singleton
class IslamicDateProvider @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val _currentDate = MutableStateFlow<LocalDate?>(null)
    val currentDateFlow: StateFlow<LocalDate?> = _currentDate.asStateFlow()

    suspend fun getCurrentDate(): LocalDate {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()
        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
            return LocalDate.now()
        }
        val location = locationPrefs.lastResolvedLocation
        return try {
            prayerTimesRepository.getIslamicCurrentDate(
                latitude = location.latitude,
                longitude = location.longitude
            )
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    suspend fun refreshDate() {
        val newDate = getCurrentDate()
        _currentDate.value = newDate
    }
}
```

- `_currentDate` starts as `null` and is updated when `refreshDate()` is called
- The flow emits non-null after the first refresh, allowing ViewModels to react
- `getCurrentDate()` remains available for one-shot callers

### ReadingViewModel - Subscribe to reactive date

```kotlin
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val localeManager: LocaleManager,
    private val islamicDateProvider: IslamicDateProvider,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val categoryId: String? = savedStateHandle["categoryId"]

    init {
        viewModelScope.launch {
            islamicDateProvider.refreshDate()
        }
    }

    private val todayFlow: Flow<String> = islamicDateProvider.currentDateFlow
        .filterNotNull()
        .map { it.toString() }

    val items: StateFlow<List<AzkarItemUi>> = localeManager.currentLangTagFlow.flatMapLatest { lang ->
        if (categoryId == null) {
            flowOf(emptyList())
        } else {
            todayFlow.flatMapLatest { today ->
                repository.observeItemsForCategory(categoryId, langTag = lang, today)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun incrementRepeat(itemId: String) {
        viewModelScope.launch {
            val id = categoryId ?: return@launch
            val today = islamicDateProvider.currentDateFlow.value?.toString()
                ?: islamicDateProvider.getCurrentDate().toString()
            repository.incrementRepeat(id, itemId, today)
        }
    }

    fun markItemComplete(itemId: String) {
        viewModelScope.launch {
            val id = categoryId ?: return@launch
            val today = islamicDateProvider.currentDateFlow.value?.toString()
                ?: islamicDateProvider.getCurrentDate().toString()
            repository.markItemComplete(id, itemId, today)
        }
    }
    // ... rest unchanged
}
```

Key change: `todayFlow` is now derived from `islamicDateProvider.currentDateFlow.filterNotNull()`, so when the date updates, all downstream flows (`items`, `weightedProgress`) automatically re-subscribe with the new date.

Write operations now read from `islamicDateProvider.currentDateFlow.value` (the same reactive source), eliminating the read/write mismatch.

### SummaryViewModel - Same reactive date pattern

Replace:
```kotlin
flow { emit(islamicDateProvider.getCurrentDate().toString()) }
```
With the same `islamicDateProvider.currentDateFlow.filterNotNull().map { it.toString() }` pattern.

Also fix `toggleCategoryCompletion` to use the reactive date:
```kotlin
val today = islamicDateProvider.currentDateFlow.value?.toString()
    ?: islamicDateProvider.getCurrentDate().toString()
```

### MainActivity - Trigger date refresh on resume

```kotlin
override fun onResume() {
    super.onResume()
    appUpdateManager.onResume()
    localeManager.notifyLocaleChanged()

    // Refresh the Islamic date when the app comes to the foreground.
    // This detects day changes (e.g., user left the app overnight).
    lifecycleScope.launch {
        islamicDateProvider.refreshDate()
    }
}
```

This requires injecting `IslamicDateProvider` into `MainActivity`.

### ReadingScreen - Guard against empty items in pager

The pager's `pageCount` lambda already handles the empty case (`items.size = 0`), but add an explicit guard to prevent any transient out-of-bounds access:

```kotlin
val pageCount = if (items.isEmpty()) 0 else if (isComplete) items.size + 1 else items.size
val pagerState = rememberPagerState(pageCount = { pageCount })
```

And in the pager content, the existing `items.isNotEmpty()` check on line 258 prevents `items[page]` from being accessed when empty. No additional change needed here, but verify the `LaunchedEffect(initialPageIndex)` doesn't attempt to scroll to an invalid page when items is empty.

## Boundary Conditions and Exception Handling

1. **First launch**: `_currentDate` starts as `null`. ViewModels use `filterNotNull()` which waits until the first `refreshDate()` call. The `initialValue` of `emptyList()` for `items` handles the gap.
2. **No location**: `getCurrentDate()` falls back to `LocalDate.now()`, which always succeeds.
3. **Prayer times unavailable**: `getCurrentDate()` catches all exceptions and falls back to `LocalDate.now()`.
4. **Rapid date transitions**: `flatMapLatest` cancels previous subscriptions when a new date arrives, preventing stale data.
5. **Process death + restoration**: `currentDateFlow` is a `StateFlow` in a `@Singleton`, so Hilt recreates it on process restoration. The initial `null` value + `filterNotNull()` + `refreshDate()` in ViewModel `init` ensures proper initialization.

## Data Flow Paths

### Current (broken) flow:
```
ReadingViewModel.todayFlow (one-shot, stale date)
  -> items StateFlow (observes progress for stale date)
  -> weightedProgress StateFlow (observes progress for stale date)

incrementRepeat/markItemComplete (calls getCurrentDate() fresh)
  -> writes progress to NEW date
  -> UI doesn't update (observing OLD date)
```

### Fixed flow:
```
IslamicDateProvider.currentDateFlow (reactive, updates on refreshDate())
  -> ReadingViewModel.todayFlow (derived from currentDateFlow)
    -> items StateFlow (observes progress for CURRENT date)
    -> weightedProgress StateFlow (observes progress for CURRENT date)

incrementRepeat/markItemComplete (reads currentDateFlow.value)
  -> writes progress to CURRENT date
  -> UI updates (observing CURRENT date)
```

## Expected Outcomes

1. App no longer crashes when reopened after a day change on the reading page
2. Progress is consistently read and written for the same date
3. When the day changes, the reading page automatically refreshes to show the new day's progress
4. Summary page also reflects the correct date
