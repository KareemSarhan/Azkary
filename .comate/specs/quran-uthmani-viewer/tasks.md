# Quran Uthmani Script Viewer - Task Plan

- [x] Task 1: Download and add KFGQPC Uthmanic Script HAFS font
    - 1.1: Download the KFGQPC Uthmanic Script HAFS Regular TTF font file
    - 1.2: Place it at `app/src/main/res/font/quran_uthmani.ttf`
    - 1.3: Verify the font file is recognized by Android resource system

- [x] Task 2: Define QuranUthmaniFont FontFamily in Type.kt
    - 2.1: Add `QuranUthmaniFont` FontFamily referencing `R.font.quran_uthmani` in `app/src/main/java/com/app/azkary/ui/theme/Type.kt`

- [x] Task 3: Create Quran item detection helper
    - 3.1: Create `app/src/main/java/com/app/azkary/util/QuranTextHelper.kt`
    - 3.2: Implement `isQuranicItem(itemId: String): Boolean` with prefixes: `itm-ikhlas-`, `itm-falaq-`, `itm-nas-`, `itm-ayat-al-kursi-`, `itm-recitation-`, `itm-ayatain-`, `itm-ikhlas-falaq-nas-`

- [x] Task 4: Update AzkarReadingItem to use Uthmani font for Quran items
    - 4.1: Import `QuranUthmaniFont` and `isQuranicItem` in `AzkarReadingItem.kt`
    - 4.2: Conditionally set `fontFamily` to `QuranUthmaniFont` when `isQuranicItem(item.id)` is true, otherwise `FontFamily.Default`

- [x] Task 5: Update short surahs and Ayat Al-Kursi to Uthmani script in items_ar.json
    - 5.1: Update `itm-ikhlas-003` text to Uthmani script (replace `ا` with `ٱ`, add proper `ۡ` marks, add verse numbers)
    - 5.2: Update `itm-falaq-003` text to Uthmani script
    - 5.3: Update `itm-nas-003` text to Uthmani script
    - 5.4: Update `itm-ayat-al-kursi-1` text to Uthmani script
    - 5.5: Update `itm-ikhlas-falaq-nas-3` text to Uthmani script
    - 5.6: Update `itm-ayatain-albaqarah-sleep-001` text to consistent Uthmani script

- [x] Task 6: Bump seed schema version
    - 6.1: Increment `schemaVersion` from 9 to 10 in `app/src/main/assets/seed/manifest.json`
    - 6.2: Update `generatedAt` timestamp

- [x] Task 7: Build verification and commit
    - 7.1: Run Gradle build to verify compilation
    - 7.2: Commit all changes with descriptive message
