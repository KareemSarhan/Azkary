# Azkary - Islamic Remembrance App

An Android application for daily Islamic remembrances (Azkar) with progress tracking, multi-language support, and a beautiful modern UI.

## 📱 Features

### Core Functionality

#### 🕌 Azkar Categories
- **Morning Azkar** (أذكار الصباح) - 25 remembrances
- **Evening Azkar** (أذكار المساء) - 22 remembrances  
- **Sleep Azkar** (أذكار النوم) - 16 remembrances
- Each category contains authentic Islamic supplications with references

#### 📖 Reading Experience
- **Full-screen reading interface** with swipeable pages
- **Arabic text** with proper RTL support
- **Transliteration** for non-Arabic speakers
- **English translations** for understanding
- **Hadith references** with source citations
- **Tap-to-increment counter** for repetition tracking
- **Auto-advance** to next dhikr when completed
- **Haptic feedback** on interactions
- **Progress bar** showing weighted completion

#### 📊 Progress Tracking
- **Daily progress monitoring** for each category
- **Weighted progress calculation** based on text length and repetitions
- **Circular progress indicators** on summary cards
- **Persistent storage** of user progress per date
- **Current session** highlighting on home screen
- **Completion status** for individual items

#### 🌍 Multi-Language Support
- **System language detection** (Arabic/English)
- **Manual language selection** via Settings
- **RTL/LTR layout** automatic switching
- **Localized content** for all categories and items
- **Bidirectional text** rendering support

#### 🎨 UI/UX Features
- **Material Design 3** theming
- **Gradient cards** for current session
- **Smooth animations** for progress changes
- **Dark theme** with custom navy color scheme
- **Beautiful typography** optimized for Arabic text
- **Responsive layouts** for different screen sizes
- **Modern card-based** design

### Technical Features

#### 💾 Data Management
- **Room Database** for local storage
- **Seeded database** with authentic azkar content
- **JSON schema validation** for data integrity
- **Version-controlled seed data** (Schema v4)
- **Foreign key relationships** for data consistency
- **Indexed queries** for performance

#### 🏗️ Architecture
- **MVVM pattern** with ViewModel and Repository
- **Dependency Injection** with Hilt/Dagger
- **Reactive streams** with Kotlin Flow
- **Coroutines** for async operations
- **Jetpack Compose** for modern UI
- **Navigation Component** for screen transitions

#### 📱 User Preferences
- **DataStore** for persistent settings
- **Language preference** storage
- **User progress** saved per date
- **Category customization** support

## 🛠️ Tech Stack

### Android Core
- **Language**: Kotlin
- **Min SDK**: 33 (Android 13)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java**: Version 17

### Libraries & Frameworks
- **UI**: Jetpack Compose with Material 3
- **Database**: Room 2.6+
- **DI**: Hilt/Dagger 2.51+
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines + Flow
- **Storage**: DataStore Preferences
- **Serialization**: Kotlinx Serialization JSON
- **Build**: Gradle with KTS, KSP

### Key Dependencies
### WIP Features
    docs/WIP_PRAYER_TIMES.md