# Quran SDK Text in Reading Items

## Requirement
Replace the hardcoded surah/ayah text in azkar reading items with text from the quran-sdk. When an item has a `QuranReference`, the reading screen should render the actual Uthmani text from the SDK using the same visual style as the Quran reading screen — individual `AyahCard`s per ayah, ayah numbers (﴿N﴾), right-aligned Uthmani text, the full Quran reading experience.

## Problem
- Items like `itm-recitation-sajdah-sleep-001` (Surah As-Sajdah) and `itm-recitation-mulk-sleep-001` (Surah Al-Mulk) have entire surahs embedded as a single giant string in the seed JSON. This is displayed as an unformatted blob.
- These items are not in `QURAN_VERSE_ITEMS`, so they get no `quranReference` and no "Read Surah" chip.
- Even items with a `quranReference` (like Ayat Al-Kursi, Ikhlas, Falaq, Nas) still render the database text, not the SDK text.

## Architecture & Technical Approach

### 1. Add missing items to QURAN_VERSE_ITEMS
- `"itm-recitation-sajdah-sleep-001"` → `QuranReference(32, null)` (full Surah As-Sajdah)
- `"itm-recitation-mulk-sleep-001"` → `QuranReference(67, null)` (full Surah Al-Mulk)

### 2. Add `quranSurah` field to `AzkarItemUi`
Add `quranSurah: QuranSurahUi? = null` to carry SDK-loaded surah data to the UI.

### 3. Enrich items in ReadingViewModel
After `observeItemsForCategory` emits, load surah data from `QuranRepository` for items that have a `quranReference`. Use a `map` operator to enrich each item.

### 4. Render Quran-style in AzkarReadingItem
When `quranSurah` is present, render the surah using the same `AyahCard` component from `QuranReadingScreen`:
- **Full surah** (`ayahNumber == null`): Show surah name header + verse count + all ayahs as individual `AyahCard`s
- **Single ayah** (`ayahNumber != null`, e.g., Ayat Al-Kursi): Show that single ayah as an `AyahCard`
- **Fallback**: If `quranSurah` is null, render existing `arabicText` as before

The `AyahCard` composable will be extracted from `QuranReadingScreen.kt` into its own file or made internal/public so it can be reused.

For full-surah items, the layout within the reading item becomes:
```
[Title]  (e.g., "قراءة سورة الملك")
[Surah name + verse count]
[AyahCard: ayah 1]
[AyahCard: ayah 2]
...
[AyahCard: ayah 30]
[Reference card]
```

For single-ayah items (e.g., Ayat Al-Kursi):
```
[Title]
[AyahCard: ayah 255]
[Transliteration]
[Translation]
[Reference card]
[Read Surah chip → navigates to full surah]
```

### 5. Hide redundant "Read Surah" chip
When the full surah is already rendered inline (`ayahNumber == null`), the "Read Surah" chip is redundant — hide it.

## Affected Files

| File | Modification Type | Details |
|------|------------------|---------|
| `data/model/UiModels.kt` | Edit | Add `quranSurah: QuranSurahUi? = null` to `AzkarItemUi` |
| `data/repository/AzkarRepository.kt` | Edit | Add 2 missing items to `QURAN_VERSE_ITEMS` |
| `ui/quran/QuranReadingScreen.kt` | Edit | Extract `AyahCard` to be reusable (make public or move to shared file) |
| `ui/reading/ReadingViewModel.kt` | Edit | Inject `QuranRepository`, enrich items with SDK surah data |
| `ui/reading/AzkarReadingItem.kt` | Edit | Render `AyahCard`s from `quranSurah` when present, hide chip for full surahs |

## Data Flow
```
AzkarRepository.observeItemsForCategory()
  → List<AzkarItemUi> (with quranReference, no quranSurah)
  → ReadingViewModel enriches with QuranRepository.getSurah()
  → List<AzkarItemUi> (with quranSurah populated)
  → AzkarReadingItem renders AyahCards when quranSurah is present
```

## Boundary Conditions
- **SDK not available**: `getSurah()` returns null → falls back to existing `arabicText` from database
- **Single ayah**: Filter to show only that ayah, keep "Read Surah" chip for full surah navigation
- **Full surah**: Show all ayahs, hide "Read Surah" chip (redundant)
- **No quranReference**: No change — render exactly as before
