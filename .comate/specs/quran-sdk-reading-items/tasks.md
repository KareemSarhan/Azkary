# Quran SDK Reading Items - Task Plan

- [ ] Task 1: Add missing Quran references to AzkarRepository
    - 1.1: Add `"itm-recitation-sajdah-sleep-001"` → `QuranReference(32, null)` to QURAN_VERSE_ITEMS
    - 1.2: Add `"itm-recitation-mulk-sleep-001"` → `QuranReference(67, null)` to QURAN_VERSE_ITEMS

- [ ] Task 2: Add quranSurah field to AzkarItemUi model
    - 2.1: Add `quranSurah: QuranSurahUi? = null` field to `AzkarItemUi` in UiModels.kt

- [ ] Task 3: Make AyahCard reusable from QuranReadingScreen
    - 3.1: Change `AyahCard` from `private` to `internal` visibility in QuranReadingScreen.kt so it can be used by AzkarReadingItem

- [ ] Task 4: Enrich reading items with SDK data in ReadingViewModel
    - 4.1: Inject `QuranRepository` into ReadingViewModel constructor
    - 4.2: Call `quranRepository.openDatabaseIfNeeded()` in init block
    - 4.3: Add enrichment logic: after items emit, load surah from SDK for each item with quranReference
    - 4.4: Apply enrichment to the `items` StateFlow chain

- [ ] Task 5: Render Quran-style AyahCards in AzkarReadingItem
    - 5.1: When `quranSurah` is present and it's a full surah (`ayahNumber == null`): show surah name header, verse count, then all ayahs as `AyahCard`s
    - 5.2: When `quranSurah` is present and it's a single ayah (`ayahNumber != null`): show that ayah as an `AyahCard`, keep transliteration/translation below
    - 5.3: Hide "Read Surah" chip when full surah is already rendered inline (`ayahNumber == null`)
    - 5.4: Keep "Read Surah" chip for single-ayah items (navigates to full surah view)
    - 5.5: Fallback: when `quranSurah` is null, render existing `arabicText` as before

- [ ] Task 6: Build, commit, and push
    - 6.1: Run `assembleDebug` and fix any compilation errors
    - 6.2: Commit and push changes
