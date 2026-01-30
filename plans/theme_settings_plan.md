# Theme Settings Implementation Plan

## Overview

This document outlines the implementation of a complete Theme Settings system for the Azkary Android
app, allowing users to customize the app's appearance with System/Light/Dark theme modes and True
Black OLED support.

## Architecture

### Data Layer

```
app/src/main/java/com/app/azkary/data/prefs/
├── ThemeMode.kt           # Enum for theme modes
├── ThemeSettings.kt       # Data class for theme preferences
└── ThemePreferencesRepository.kt  # DataStore persistence
```

### UI Layer

```
app/src/main/java/com/app/azkary/ui/theme/
├── Theme.kt               # Updated AppTheme composable
└── TrueBlackColorScheme.kt  # OLED-friendly dark color scheme

app/src/main/java/com/app/azkary/ui/settings/
├── SettingsScreen.kt      # Theme section UI
└── SettingsViewModel.kt   # Theme settings state management
```

## Implementation Details

### 1. ThemeMode Enum

- `SYSTEM` - Follows system theme (default)
- `LIGHT` - Always light mode
- `DARK` - Always dark mode

### 2. ThemeSettings Data Class

```kotlin
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useTrueBlack: Boolean = false
)
```

### 3. ThemePreferencesRepository

- Uses separate DataStore named "theme_settings"
- Exposes `Flow<ThemeSettings>`
- Provides `setThemeMode()` and `setUseTrueBlack()` methods

### 4. True Black Color Scheme

- Pure black (#000000) for background and surface
- Maintains primary/secondary/tertiary colors from existing scheme
- Optimized for OLED displays

### 5. AppTheme Composable

- Changed signature to accept `themeSettings: ThemeSettings`
- Determines effective dark mode based on themeMode + system theme
- Applies True Black color scheme when darkTheme && useTrueBlack
- Keeps dynamic color support for Android 12+

### 6. Settings UI

- Radio buttons for System/Light/Dark selection
- True Black toggle appears only when Dark is selected
- Uses existing CustomIosToggle component

## String Resources

### English (values/strings.xml)

- `settings_section_theme` - "THEME"
- `settings_true_black_oled` - "True Black (OLED)"
- `settings_true_black_description` - "Pure black background for OLED screens"
- `theme_system` - "System Default"
- `theme_light` - "Light"
- `theme_dark` - "Dark"

### Arabic (values-ar/strings.xml)

- `settings_section_theme` - "المظهر"
- `settings_true_black_oled` - "أسود حقيقي (OLED)"
- `settings_true_black_description` - "خلفية سوداء نقية لشاشات OLED"
- `theme_system` - "النظام الافتراضي"
- `theme_light` - "فاتح"
- `theme_dark` - "داكن"

## Files Modified/Created

### Created

- `app/src/main/java/com/app/azkary/data/prefs/ThemeMode.kt`
- `app/src/main/java/com/app/azkary/data/prefs/ThemeSettings.kt`
- `app/src/main/java/com/app/azkary/data/prefs/ThemePreferencesRepository.kt`
- `app/src/main/java/com/app/azkary/ui/theme/TrueBlackColorScheme.kt`

### Modified

- `app/src/main/java/com/app/azkary/ui/theme/Theme.kt`
- `app/src/main/java/com/app/azkary/MainActivity.kt`
- `app/src/main/java/com/app/azkary/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/app/azkary/ui/settings/SettingsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ar/strings.xml`

## Dependencies

- Jetpack Compose Material 3
- DataStore Preferences
- Hilt for dependency injection
- Coroutines Flow for reactive state management