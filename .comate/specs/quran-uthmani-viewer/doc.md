# Quran Uthmani Script Viewer

## Problem Statement

Quran surahs in the app are displayed using the system default font (`FontFamily.Default`), which does not properly render Uthmani (Othmani) script characters. The seed data for full surahs (As-Sajdah, Al-Mulk, last 2 verses of Al-Baqarah) already contains Uthmani Unicode characters (e.g., `ٱ` U+0671, `ۡ` U+06E1, `۩` U+06E9, `۞` U+06DE), but the system font renders them incorrectly or with missing glyphs. Short surahs (Al-Ikhlas, Al-Falaq, An-Nas) use standard Arabic with diacritics rather than proper Uthmani script, which is also inconsistent.

## Requirement

Display all Quran text in the app using a proper Uthmani-capable font that correctly renders Quranic annotation marks, small diacritical characters, and the distinctive Uthmani letterforms, matching the traditional Quranic manuscript style.

## Affected Quran Items

| Item ID | Content | Current Script |
|---------|---------|---------------|
| `itm-ikhlas-003` | Surah Al-Ikhlas | Standard Arabic with diacritics |
| `itm-falaq-003` | Surah Al-Falaq | Standard Arabic with diacritics |
| `itm-nas-003` | Surah An-Nas | Standard Arabic with diacritics |
| `itm-ayat-al-kursi-1` | Ayat Al-Kursi | Standard Arabic with diacritics |
| `itm-recitation-sajdah-sleep-001` | Surah As-Sajdah | Uthmani (already) |
| `itm-recitation-mulk-sleep-001` | Surah Al-Mulk | Uthmani (already) |
| `itm-ayatain-albaqarah-sleep-001` | Last 2 verses of Al-Baqarah | Mixed |

## Architecture & Technical Approach

### Font Selection: KFGQPC Uthmanic Script HAFS

The **KFGQPC Uthmanic Script HAFS** font (by the King Fahd Glorious Quran Printing Complex) is the standard font for rendering Uthmani Quranic text. It is:
- Specifically designed for Quranic Uthmani script
- Supports all Quranic annotation marks and small characters
- Widely used in Quran apps (quran.com, quran-ios, etc.)
- Available as a free TTF font from KFGQPC

**Alternative considered:** Amiri font - a general-purpose Arabic font that supports Uthmani glyphs. Rejected because KFGQPC is specifically optimized for Quranic text rendering with better glyph positioning for small diacritical marks and annotation signs.

### Detection Strategy: ItemId-based Quran Identification

Rather than requiring a database schema change (adding `isQuranic` field), Quran items will be identified by a helper function that checks the `itemId` against known Quran item ID prefixes:

- `itm-ikhlas-*`
- `itm-falaq-*`
- `itm-nas-*`
- `itm-ayat-al-kursi-*`
- `itm-recitation-*`
- `itm-ayatain-*`

This approach requires no schema migration and covers all current and future Quran items following the established naming convention.

### Data Flow

```
AzkarReadingItem composable
  |
  v
isQuranicItem(itemId) helper --> true/false
  |
  v
If Quran: use QuranUthmaniFont (KFGQPC)
If Not:   use FontFamily.Default (system)
```

## Implementation Details

### 1. Add KFGQPC Uthmanic Script Font

**File to create:** `app/src/main/res/font/quran_uthmani.ttf`

Download the KFGQPC Uthmanic Script HAFS TTF font and place it in `res/font/`. Android will automatically generate the FontFamily resource.

### 2. Define FontFamily in Theme

**File to modify:** `app/src/main/java/com/app/azkary/ui/theme/Type.kt`

Add a `QuranUthmaniFont` FontFamily definition:

```kotlin
val QuranUthmaniFont = FontFamily(
    Font(R.font.quran_uthmani)
)
```

### 3. Create Quran Item Detection Helper

**File to create:** `app/src/main/java/com/app/azkary/util/QuranTextHelper.kt`

```kotlin
fun isQuranicItem(itemId: String): Boolean {
    val quranPrefixes = listOf(
        "itm-ikhlas-",
        "itm-falaq-",
        "itm-nas-",
        "itm-ayat-al-kursi-",
        "itm-recitation-",
        "itm-ayatain-"
    )
    return quranPrefixes.any { itemId.startsWith(it) }
}
```

### 4. Update AzkarReadingItem Composable

**File to modify:** `app/src/main/java/com/app/azkary/ui/reading/AzkarReadingItem.kt`

Change the Arabic text rendering to conditionally use the Uthmani font:

```kotlin
// Arabic Text
item.arabicText?.let { arabic ->
    val fontFamily = if (isQuranicItem(item.id)) {
        QuranUthmaniFont
    } else {
        FontFamily.Default
    }
    Text(
        text = arabic,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 28.sp,
            lineHeight = 42.sp,
            fontFamily = fontFamily,
            color = colors.onBackground
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp)
    )
}
```

### 5. Update Short Surahs to Uthmani Script

**File to modify:** `app/src/main/assets/seed/items/items_ar.json`

Update the text for short surahs to use proper Uthmani script with the correct Unicode characters:

- **Surah Al-Ikhlas** (`itm-ikhlas-003`): Convert from standard Arabic to Uthmani script
- **Surah Al-Falaq** (`itm-falaq-003`): Convert from standard Arabic to Uthmani script
- **Surah An-Nas** (`itm-nas-003`): Convert from standard Arabic to Uthmani script
- **Ayat Al-Kursi** (`itm-ayat-al-kursi-1`): Convert from standard Arabic to Uthmani script

Also update the combined item `itm-ikhlas-falaq-nas-3` if it exists in the data.

Example conversion for Al-Ikhlas:
- **Current:** `قُلْ هُوَ اللَّهُ أَحَدٌ، اللَّهُ الصَّمَدُ، لَمْ يَلِدْ وَلَمْ يُولَدْ، وَلَمْ يَكُن لَهُ كُفُوًا أَحَدٌ.`
- **Uthmani:** `قُلۡ هُوَ ٱللَّهُ أَحَدٌ程度上 ٱللَّهُ ٱلصَّمَدُ...` (with proper Uthmani Alef `ٱ`, small diacritical marks `ۡ`, etc.)

### 6. Bump Seed Schema Version

**File to modify:** `app/src/main/assets/seed/manifest.json`

Increment the schema version so the `SeedManager` re-imports the updated Uthmani text on existing user installations.

## Boundary Conditions & Exception Handling

1. **Font not loaded:** If the TTF font file is missing or corrupted, Compose will fall back to the system default font - no crash.
2. **Existing users:** The seed schema version bump ensures the updated Uthmani text replaces the old text in the Room database. The `SeedManager` already handles re-seeding when the schema version changes.
3. **New Quran items:** Any future Quran items following the `itm-recitation-*`, `itm-ayatain-*`, or `itm-ayat-al-kursi-*` naming convention will automatically use the Uthmani font.
4. **User-created items:** Will always use `FontFamily.Default` since they don't match any Quran item ID prefix.

## Affected Files Summary

| File | Modification Type | Description |
|------|------------------|-------------|
| `app/src/main/res/font/quran_uthmani.ttf` | CREATE | KFGQPC Uthmanic Script HAFS font file |
| `app/src/main/java/com/app/azkary/ui/theme/Type.kt` | MODIFY | Add `QuranUthmaniFont` FontFamily |
| `app/src/main/java/com/app/azkary/util/QuranTextHelper.kt` | CREATE | `isQuranicItem()` helper function |
| `app/src/main/java/com/app/azkary/ui/reading/AzkarReadingItem.kt` | MODIFY | Conditional font selection for Arabic text |
| `app/src/main/assets/seed/items/items_ar.json` | MODIFY | Update short surahs to Uthmani script |
| `app/src/main/assets/seed/manifest.json` | MODIFY | Bump schema version |

## Expected Outcome

- Quran surahs display with proper Uthmani script rendering using the KFGQPC font
- All Uthmani-specific glyphs (small diacritical marks, Quranic annotation signs, Uthmani Alef) render correctly
- Non-Quran Arabic text (duas, dhikr) continues to use the system default font
- Short surahs now contain proper Uthmani script text matching the traditional Quranic manuscript
- Existing users receive the updated text through seed re-import
