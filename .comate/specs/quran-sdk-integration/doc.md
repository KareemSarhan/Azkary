# Quran SDK Integration — Spec Document

## Overview

Integrate `com.tazkiyatech:quran-sdk:3.0.0` into Azkary to add offline Quran verse access, enhancing the app's Islamic content beyond dhikr/remembrance. The SDK ships with an embedded SQLite database providing surah names, ayah text, and Quran metadata — all offline, which aligns with Azkary's privacy-first, no-internet philosophy.

## Why This Aligns With Azkary

- Azkary already contains several azkar items that ARE Quran verses (Ayat al-Kursi = 2:255, last two verses of Al-Baqarah = 2:285-286, Surat al-Ikhlas/Falaq/Nas = 112-114), but the text is hardcoded in seed JSON with no way to browse the full surah or see the verse in context.
- The SDK is 100% offline — consistent with Azkary's "no internet required after initial setup" promise.
- Apache 2.0 license — compatible with Azkary's GPL-3.0.
- minSdk 26, compileSdk 36 — compatible with Azkary's minSdk 24 (quran-sdk's native DB works on API 26+, but Azkary desugars to 24; the SDK's AAR will load fine since Room/SQLite handles backward compat).

## SDK API Surface

```kotlin
// Entry point — constructed with Application context
val quranDb = QuranDatabase(applicationContext)

// Must be called on a background thread before first access (or auto-opens on first get)
quranDb.openDatabase()

// Surah access
quranDb.getSurahNames(): List<String>                     // 114 surah names
quranDb.getSurahName(surahNumber: Int): String            // single surah name
quranDb.getAyahsInSurah(surahNumber: Int): List<String>  // all ayahs in a surah
quranDb.getAyah(surahNumber: Int, ayahNumber: Int): String  // single ayah

// Metadata
quranDb.getSectionMetadata(SectionType): List<SectionMetadata>
quranDb.getSectionMetadata(SectionType, sectionNumber: Int): SectionMetadata

// SectionType enum: SURAH, JUZ_IN_MADINAH_MUSHAF, HIZB_IN_MADINAH_MUSHAF,
//   HIZB_QUARTER_IN_MADINAH_MUSHAF, JUZ_IN_MAJEEDI_MUSHAF, JUZ_QUARTER_IN_MAJEEDI_MUSHAF

// SectionMetadata: sectionType, sectionNumber, numAyahs, surahNumber, ayahNumber

// Bonus
QuranQuotes().getRandomQuote(): String
HifdhTips().getRandomTip(): String
```

## Features to Implement

### Feature 1: Quran Verse of the Day on Summary Screen

**Scenario**: On the home/summary screen, display a Quran verse that refreshes daily. This provides spiritual enrichment alongside the existing azkar tracking.

**Processing logic**:
- Use `QuranDatabase` to pick a verse deterministically based on the current Islamic date
- The verse selection uses surah+ayah from the Islamic date as a seed (deterministic per day, same verse for all users on the same day)
- Display in a Card below the weekly progress section
- Tapping the card navigates to the full Surah reading view (Feature 2)

**Data flow**:
```
IslamicDateProvider.getCurrentDate() -> hash to (surahNumber, ayahNumber)
  -> QuranDatabase.getAyah(surah, ayah) + QuranDatabase.getSurahName(surah)
  -> VerseOfDayUi(surahName, ayahText, surahNumber, ayahNumber)
  -> SummaryScreen composable
```

### Feature 2: Quran Surah Reading Screen

**Scenario**: A new screen to read a full surah, navigable from the verse-of-day card or from azkar items that contain Quran verses.

**Processing logic**:
- New navigation route: `"quran/{surahNumber}"`
- Load `getAyahsInSurah(surahNumber)` and `getSurahName(surahNumber)`
- Display as a scrollable list of ayah cards with Arabic text
- Optional: show ayah numbers from `SectionMetadata`
- Back navigation returns to the previous screen

**Data flow**:
```
Navigation argument: surahNumber (Int)
  -> QuranRepository.getSurah(surahNumber)
  -> QuranSurahUi(surahNumber, surahName, List<AyahUi>)
  -> QuranReadingScreen composable
```

### Feature 3: "Read Full Surah" Link From Azkar Items

**Scenario**: When an azkar item's `arabicText` contains a known Quran surah/verse, show a "Read full surah" chip that navigates to the Quran reading screen.

**Processing logic**:
- Add an optional `quranReference` field to `AzkarItemUi` — structured as `QuranReference(surahNumber: Int, ayahNumber: Int?)?`
- Map known azkar items to their Quran references in the repository layer (hardcoded mapping for the 7 known Quran-verse items)
- In `AzkarReadingItem`, if `item.quranReference != null`, show a clickable chip below the reference card
- On click: navigate to `"quran/{surahNumber}"`

**Known Quran-verse azkar items** (from seed data):

| itemId | Surah | Ayah(s) |
|--------|-------|---------|
| `itm-ayat-al-kursi-1` | 2 | 255 |
| `itm-last-two-baqara-1` | 2 | 285-286 |
| `itm-ikhlas-003` | 112 | 1-4 |
| `itm-falaq-003` | 113 | 1-5 |
| `itm-nas-003` | 114 | 1-6 |
| `itm-ikhlas-falaq-nas-3` | 112-114 | (multiple) |
| `itm-ayatain-albaqarah-sleep-001` | 2 | 285-286 |

**Data flow**:
```
AzkarRepository.observeItemsForCategory() -> maps known items to QuranReference
  -> AzkarItemUi.quranReference
  -> AzkarReadingItem composable shows "Read Surah" chip
  -> Navigation to QuranReadingScreen
```

## Architecture & Technical Approach

### New Files

| File | Type | Purpose |
|------|------|---------|
| `data/quran/QuranRepository.kt` | New | Wrapper around `QuranDatabase`, provides reactive access to Quran data |
| `ui/quran/QuranReadingViewModel.kt` | New | ViewModel for surah reading screen |
| `ui/quran/QuranReadingScreen.kt` | New | Composable screen for reading a surah |
| `di/QuranModule.kt` | New | Hilt module providing `QuranDatabase` and `QuranRepository` as singletons |

### Modified Files

| File | Modification | Details |
|------|-------------|---------|
| `gradle/libs.versions.toml` | Add entry | `quran-sdk = "3.0.0"` version + library entry |
| `app/build.gradle.kts` | Add dependency | `implementation(libs.quran.sdk)` |
| `data/model/UiModels.kt` | Add fields | Add `QuranReference` data class, add `quranReference` field to `AzkarItemUi` |
| `data/repository/AzkarRepository.kt` | Modify | Map known items to `QuranReference` in `observeItemsForCategory()` |
| `ui/reading/AzkarReadingItem.kt` | Modify | Show "Read Surah" chip when `quranReference != null` |
| `ui/summary/SummaryViewModel.kt` | Modify | Add `verseOfDay: StateFlow<VerseOfDayUi?>` |
| `ui/summary/SummaryScreen.kt` | Modify | Add verse-of-day card, navigation to Quran screen |
| `MainActivity.kt` | Modify | Add `"quran/{surahNumber}"` route to NavHost |

### Hilt DI Setup

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object QuranModule {

    @Provides
    @Singleton
    fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase {
        return QuranDatabase(context)
    }

    @Provides
    @Singleton
    fun provideQuranRepository(quranDatabase: QuranDatabase): QuranRepository {
        return QuranRepository(quranDatabase)
    }
}
```

### QuranRepository Design

```kotlin
class QuranRepository(private val quranDatabase: QuranDatabase) {

    suspend fun getSurah(surahNumber: Int): QuranSurahUi {
        val name = quranDatabase.getSurahName(surahNumber)
        val ayahs = quranDatabase.getAyahsInSurah(surahNumber)
        val metadata = quranDatabase.getSectionMetadata(SectionType.SURAH, surahNumber)
        return QuranSurahUi(
            surahNumber = surahNumber,
            surahName = name,
            ayahs = ayahs.mapIndexed { index, text ->
                AyahUi(ayahNumber = index + 1, text = text)
            },
            totalAyahs = metadata.numAyahs
        )
    }

    fun getVerseOfDay(islamicDate: String): VerseOfDayUi {
        // Deterministic selection: hash the date to pick surah+ayah
        val seed = islamicDate.hashCode().toLong()
        val surahNumber = (abs(seed) % 114 + 1).toInt()  // 1-114
        val ayahs = quranDatabase.getAyahsInSurah(surahNumber)
        val ayahIndex = (abs(seed / 114) % ayahs.size).toInt()
        val surahName = quranDatabase.getSurahName(surahNumber)
        return VerseOfDayUi(
            surahName = surahName,
            ayahText = ayahs[ayahIndex],
            surahNumber = surahNumber,
            ayahNumber = ayahIndex + 1
        )
    }

    fun getRandomQuote(): String = QuranQuotes().getRandomQuote()
    fun getRandomHifdhTip(): String = HifdhTips().getRandomTip()
}
```

### Quran-Verse Azkar Mapping (in AzkarRepository)

```kotlin
private val QURAN_VERSE_ITEMS = mapOf(
    "itm-ayat-al-kursi-1" to QuranReference(2, 255),
    "itm-last-two-baqara-1" to QuranReference(2, 285),
    "itm-ikhlas-003" to QuranReference(112, null),
    "itm-falaq-003" to QuranReference(113, null),
    "itm-nas-003" to QuranReference(114, null),
    "itm-ikhlas-falaq-nas-3" to QuranReference(112, null), // primary surah
    "itm-ayatain-albaqarah-sleep-001" to QuranReference(2, 285),
)
```

### New UI Models

```kotlin
data class QuranReference(val surahNumber: Int, val ayahNumber: Int?)

data class VerseOfDayUi(
    val surahName: String,
    val ayahText: String,
    val surahNumber: Int,
    val ayahNumber: Int
)

data class QuranSurahUi(
    val surahNumber: Int,
    val surahName: String,
    val ayahs: List<AyahUi>,
    val totalAyahs: Int
)

data class AyahUi(
    val ayahNumber: Int,
    val text: String
)
```

## Boundary Conditions & Exception Handling

1. **SDK database open failure**: `QuranDatabase.openDatabase()` can throw `IOException` or `QuranDatabaseException`. Wrap in try-catch in `QuranRepository`, return null/empty on failure. The app should degrade gracefully — verse-of-day simply doesn't show, "Read Surah" chip doesn't appear.

2. **Invalid surah/ayah numbers**: SDK throws on out-of-range surah (< 1 or > 114) or ayah numbers. `QuranRepository` must validate before calling SDK methods.

3. **First-time initialization latency**: `openDatabase()` copies the embedded DB from assets on first call (several MB). This must happen on a background thread. Do it in `AzkaryApp.onCreate()` alongside existing seed logic, or lazily on first access in `QuranRepository`.

4. **Compressible verse-of-day**: If the SDK fails to open or the verse fetch fails, `verseOfDay` StateFlow emits `null` and the UI simply hides the card. No crash, no empty card.

5. **RTL rendering**: Quran ayah text from the SDK is Arabic. The Quran reading screen must use RTL layout, same as existing Arabic azkar rendering patterns.

6. **ProGuard/R8**: The SDK ships its own ProGuard rules in the AAR. Since Azkary already enables `isMinifyEnabled = true` for release builds, verify the SDK's rules are consumed correctly. Add keep rules if needed.

7. **`itm-ikhlas-falaq-nas-3`** references 3 surahs. The mapping points to surah 112 as the "primary". The chip should say "Read Surah Al-Ikhlas" (the first one).

## Data Flow Paths

### Verse of the Day
```
AzkaryApp.onCreate()
  -> QuranDatabase opened lazily (or eagerly in background)

SummaryViewModel.init
  -> islamicDateProvider.getCurrentDate()
  -> quranRepository.getVerseOfDay(date)
  -> VerseOfDayUi emitted to verseOfDay StateFlow

SummaryScreen
  -> Collects verseOfDay
  -> Renders VerseOfDayCard (or hides if null)
  -> onClick -> navigation to "quran/{surahNumber}"
```

### Read Full Surah from Azkar
```
AzkarReadingItem
  -> item.quranReference != null
  -> Renders "Read Surah {name}" chip
  -> onClick -> navigation to "quran/{surahNumber}"

QuranReadingViewModel
  -> quranRepository.getSurah(surahNumber)
  -> QuranSurahUi emitted to surah StateFlow

QuranReadingScreen
  -> Collects surah
  -> Renders scrollable list of AyahUi cards
  -> Back button returns to previous screen
```

## Expected Outcomes

1. **Gradle dependency added** — `com.tazkiyatech:quran-sdk:3.0.0` compiles and syncs successfully
2. **Verse of the Day** appears on the summary screen, deterministic per Islamic date
3. **"Read Surah" chip** appears on azkar items that contain Quran verses, linking to the full surah
4. **Quran Reading screen** displays any surah's ayahs in a scrollable Arabic layout
5. **Graceful degradation** — if SDK fails to open, the app works exactly as before (no crashes, no blank areas)
6. **No internet required** — all Quran content is served from the SDK's embedded offline database
7. **All existing functionality** remains unchanged — this is purely additive
