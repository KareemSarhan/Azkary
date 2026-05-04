# Quran SDK Integration — Task Plan

- [x] Task 1: Add quran-sdk Gradle dependency
    - 1.1: Add `quranSdk = "3.0.0"` version entry to `gradle/libs.versions.toml` `[versions]` section
    - 1.2: Add `quran-sdk = { group = "com.tazkiyatech", name = "quran-sdk", version.ref = "quranSdk" }` to `[libraries]` section
    - 1.3: Add `implementation(libs.quran.sdk)` to `app/build.gradle.kts` dependencies block
    - 1.4: Run Gradle sync to verify dependency resolves successfully

- [x] Task 2: Create QuranRepository and Hilt DI module
    - 2.1: Create `data/model/QuranModels.kt` with `QuranReference`, `VerseOfDayUi`, `QuranSurahUi`, `AyahUi` data classes
    - 2.2: Create `data/quran/QuranRepository.kt` wrapping `QuranDatabase` with `getSurah()`, `getVerseOfDay()`, `openDatabaseIfNeeded()` methods; wrap all SDK calls in try-catch returning null on failure
    - 2.3: Create `di/QuranModule.kt` Hilt `@Module` providing `QuranDatabase` and `QuranRepository` as `@Singleton`
    - 2.4: Verify project compiles with new module

- [x] Task 3: Add Quran reference mapping to AzkarItemUi
    - 3.1: Add `quranReference: QuranReference? = null` field to `AzkarItemUi` in `data/model/UiModels.kt`
    - 3.2: Add the `QURAN_VERSE_ITEMS` mapping (7 known items -> QuranReference) as a private constant in `AzkarRepository`
    - 3.3: Modify `AzkarRepository.observeItemsForCategory()` to look up `quranReference` from the mapping and include it when building `AzkarItemUi`
    - 3.4: Verify existing reading screen still renders correctly (quranReference is nullable, no visual change yet)

- [x] Task 4: Create Quran Reading screen and ViewModel
    - 4.1: Create `ui/quran/QuranReadingViewModel.kt` — `@HiltViewModel` injecting `QuranRepository`, with `surahNumber` from SavedStateHandle, exposing `surahUi: StateFlow<QuranSurahUi?>`
    - 4.2: Create `ui/quran/QuranReadingScreen.kt` — Composable with TopAppBar (surah name as title), scrollable Column of ayah cards with Arabic text and ayah numbers, RTL layout, back navigation
    - 4.3: Add `"quran/{surahNumber}"` route to NavHost in `MainActivity.kt` with `QuranReadingViewModel` and `QuranReadingScreen`
    - 4.4: Add string resources for Quran screen labels (surah, ayah, etc.) in EN and AR

- [x] Task 5: Add "Read Surah" chip to AzkarReadingItem
    - 5.1: Modify `ui/reading/AzkarReadingItem.kt` — when `item.quranReference != null`, render a clickable "Read Surah" chip below the reference card
    - 5.2: Add `onNavigateToQuran: (Int) -> Unit` callback parameter to `AzkarReadingItem` and propagate through `ReadingScreen`
    - 5.3: Wire `ReadingScreen`'s `onNavigateToQuran` to navigate to `"quran/{surahNumber}"` via the NavController
    - 5.4: Add string resources for "Read Surah" / "اقرأ السورة" in EN and AR values files

- [x] Task 6: Add Verse of the Day to Summary screen
    - 6.1: Inject `QuranRepository` into `SummaryViewModel`; add `verseOfDay: StateFlow<VerseOfDayUi?>` property that derives from `islamicDateProvider.getCurrentDate()`
    - 6.2: Add `VerseOfDayCard` composable to `ui/summary/SummaryScreen.kt` — displays ayah text with surah name, styled card matching existing Material 3 theme, clickable to navigate to Quran screen
    - 6.3: Place `VerseOfDayCard` below the weekly progress section, only visible when `verseOfDay != null`
    - 6.4: Add `onNavigateToQuran: (Int) -> Unit` callback to `SummaryScreen` and wire to NavController for `"quran/{surahNumber}"` route
    - 6.5: Add string resources for verse-of-day labels ("Verse of the Day" / "آية اليوم") in EN and AR

- [x] Task 7: Initialize QuranDatabase on app startup
    - 7.1: Inject `QuranRepository` into `AzkaryApp`; call `quranRepository.openDatabaseIfNeeded()` in the `applicationScope.launch` block in `onCreate()` alongside existing seed and notification scheduling
    - 7.2: Verify the SDK database copies from assets successfully on fresh install (the ~3MB embedded DB)
    - 7.3: Test that the app launches without regression when the SDK DB is unavailable (graceful fallback)

- [x] Task 8: Build verification and final integration test
    - 8.1: Run `gradlew assembleDebug` to verify full project compiles
    - 8.2: Run `gradlew test` to verify existing unit tests still pass
    - 8.3: Manual verification checklist: verse-of-day card appears on summary screen, "Read Surah" chip appears on known Quran-verse azkar items, Quran reading screen displays surah ayahs, back navigation works, app works offline
    - 8.4: Update PLAN.md or remove it since all items are now implemented
