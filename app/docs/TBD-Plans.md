# Completed Features

## Core Screens

- **Summary Screen** - Home screen showing current session card and today's categories with progress
  rings
- **Reading Screen** - Zikr reading with pager, repeat counter, weighted progress, auto-scroll,
  vibration feedback
- **Settings Screen** - Prayer times location, language, theme settings

## Prayer Times

- **Aladhan API Integration** - Fetch monthly prayer times based on location
- **Location-based Prayer Times** - GPS location support for prayer time calculation
- **Azkar Window Engine** - Morning (Fajr-Asr), Night (Asr-Isha), Sleep (Isha-Fajr) time windows

## Settings

- **Theme Settings** - System/Light/Dark mode with True Black OLED option
- **Language Settings** - Per-app language selection (English, Arabic)
- **Location Settings** - Enable/disable location for prayer times with refresh
- **Support/Feedback** - Send device/app info for support

## Progress Tracking

- **Weighted Progress** - Progress based on zikr item weights (repeats)
- **Session Resumption** - Continue from last position in partially completed category
- **Category Progress** - Visual progress rings for each category

## Database & Data

- **Room Database** - Local storage for categories, items, texts, progress, prayer times
- **Multi-language Content** - English and Arabic azkar content
- **Seed Data** - Initial azkar data loaded from JSON

## UI/UX

- **Material 3 Design** - Modern Material You design
- **RTL Support** - Full Arabic RTL support
- **Vibration Feedback** - Haptic feedback on zikr completion
- **Progress Visualization** - Circular progress indicators

## Themes

- **System Theme** - Follow device system settings
- **Light Theme** - Light color scheme
- **Dark Theme** - Dark color scheme with True Black OLED option
- **Theme Persistence** - Theme preference saved in preferences

## Zikr Content Display

- **Original Arabic Text** - Display Arabic text with proper RTL layout
- **Transliteration** - Latin transliteration of Arabic text
- **Translation** - English translation of zikr meanings
- **Reference** - Source/hadith reference for each zikr
- **Multi-language Support** - Content available in Arabic and English

---

# TBD (To Be Developed)

## Current Zikr Display Logic

- Only show current zikr if there's a zikr category in the current time window that isn't completed

## Zikr Category Priority

- Add priority field to zikr category (separate from order)
- Priority determines current zikr precedence, order is for listing

## Post Prayer Azkar

- Add post-prayer azkar for each prayer
- Post-prayer azkar always takes priority over morning/night azkar

## Hold Functionality - Category

- Long-press on zikr category marks it complete
- Long-press on completed category marks it incomplete

## Hold Functionality - Zikr Reading

- Long-press on zikr marks it complete
- Add settings option to enable/disable hold-to-complete

## Completion Page

- Show completion screen when zikr category reading is finished

## Resume Partial Reading

- Start from last completed zikr when reopening partially completed category

## Donation Page

- Add donation page in settings

## About Page

- Add about me page in settings

## Seed File Split

- Split seed_azkar.json by language for easier i18n support