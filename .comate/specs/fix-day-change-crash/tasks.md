# Fix: Day-Change Crash on Reading Page

- [x] Task 1: Add reactive date StateFlow to IslamicDateProvider
    - 1.1: Add `_currentDate: MutableStateFlow<LocalDate?>` field initialized to `null`
    - 1.2: Add `currentDateFlow: StateFlow<LocalDate?>` public read-only accessor
    - 1.3: Add `suspend fun refreshDate()` that calls `getCurrentDate()` and updates `_currentDate.value`
    - 1.4: Keep existing `getCurrentDate()` untouched for backward compatibility

- [x] Task 2: Update ReadingViewModel to use reactive date flow
    - 2.1: Replace `todayFlow` from `flow { emit(...) }` to `islamicDateProvider.currentDateFlow.filterNotNull().map { it.toString() }`
    - 2.2: Add `init` block that calls `islamicDateProvider.refreshDate()` in `viewModelScope.launch`
    - 2.3: Update `incrementRepeat()` to read date from `islamicDateProvider.currentDateFlow.value` with fallback to `getCurrentDate()`
    - 2.4: Update `markItemComplete()` same as incrementRepeat
    - 2.5: Add required imports (`filterNotNull`, `map`)

- [x] Task 3: Update SummaryViewModel to use reactive date flow
    - 3.1: Replace one-shot `flow { emit(islamicDateProvider.getCurrentDate().toString()) }` in `categories` with `islamicDateProvider.currentDateFlow.filterNotNull().map { it.toString() }`
    - 3.2: Update `toggleCategoryCompletion()` to read date from `islamicDateProvider.currentDateFlow.value` with fallback to `getCurrentDate()`
    - 3.3: Add `init` block that calls `islamicDateProvider.refreshDate()` in `viewModelScope.launch` (if not already refreshing via existing `locationPreferencesJob`)
    - 3.4: Add required imports

- [x] Task 4: Add date refresh on app resume in MainActivity
    - 4.1: Inject `IslamicDateProvider` into `MainActivity`
    - 4.2: In `onResume()`, launch `lifecycleScope.launch { islamicDateProvider.refreshDate() }`
    - 4.3: Add required imports (`lifecycleScope`, `launch`)

- [x] Task 5: Build and verify
    - 5.1: Run `./gradlew assembleDebug` in the worktree to verify compilation
    - 5.2: Run existing unit tests to verify no regressions
