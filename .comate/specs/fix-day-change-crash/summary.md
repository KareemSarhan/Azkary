# Summary: Fix Day-Change Crash on Reading Page

## Problem
When the user left the app on a zikr reading page and the day changed (e.g., overnight), reopening the app crashed. The root cause was that `todayFlow` in `ReadingViewModel` was a one-shot cold flow (`flow { emit(...) }`) that emitted the Islamic date once and never re-emitted.

**Crash scenario:**
1. User opens reading screen on Day A - `todayFlow` emits "Day A"
2. App goes to background, Android kills the process
3. Day changes to Day B
4. User reopens the app - ViewModel is recreated
5. `items` StateFlow starts with `initialValue = emptyList()` (pageCount = 0)
6. `HorizontalPager` has `currentPage` restored from saved state > 0 but `pageCount` is 0
7. Crash: `IllegalArgumentException` / `IndexOutOfBoundsException`

**Secondary bug - read/write date mismatch:**
- Reads (`items`, `weightedProgress`) used the stale `todayFlow` date
- Writes (`incrementRepeat`, `markItemComplete`) called `getCurrentDate()` fresh
- Result: user taps appeared to have no effect (progress written to new date, UI observing old date)

## Solution
1. **IslamicDateProvider** - Added `currentDateFlow: StateFlow<LocalDate?>` (reactive) and `refreshDate()` method
2. **ReadingViewModel** - Replaced one-shot `todayFlow` with `islamicDateProvider.currentDateFlow.filterNotNull().map { it.toString() }`; write operations now read from `currentDateFlow.value`
3. **SummaryViewModel** - Same reactive date pattern applied
4. **MainActivity** - Added `islamicDateProvider.refreshDate()` in `onResume()` to detect day changes when app returns to foreground

## Files Modified
| File | Change |
|------|--------|
| `IslamicDateProvider.kt` | Added `_currentDate: MutableStateFlow<LocalDate?>`, `currentDateFlow: StateFlow<LocalDate?>`, `refreshDate()` |
| `ReadingViewModel.kt` | Replaced one-shot `todayFlow` with reactive flow from `currentDateFlow`; added `refreshDate()` in `init`; updated `incrementRepeat`/`markItemComplete` to read from `currentDateFlow.value` |
| `SummaryViewModel.kt` | Same reactive date pattern; added `refreshDate()` in `init`; updated `toggleCategoryCompletion` to read from `currentDateFlow.value` |
| `MainActivity.kt` | Injected `IslamicDateProvider`; added `lifecycleScope.launch { islamicDateProvider.refreshDate() }` in `onResume()` |
| `IslamicDateProviderTest.kt` | Added tests for `currentDateFlow` (null initially) and `refreshDate()` (updates flow, fallback on error) |
| `ReadingViewModelTest.kt` | Added `currentDateFlow` and `refreshDate()` mocks to setup |
| `SummaryViewModelTest.kt` | Added `currentDateFlow` and `refreshDate()` mocks to setup |

## Verification
- Build: `assembleDebug` passed (exit code 0)
- Tests: All 3 test suites passed (`IslamicDateProviderTest`, `ReadingViewModelTest`, `SummaryViewModelTest`)
- Commit: `6c1f4b8` on branch `fix/day-change-crash` in worktree `.worktrees/fix-day-change-crash`
