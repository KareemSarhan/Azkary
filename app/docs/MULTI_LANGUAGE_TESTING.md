# Multi-Language Support Testing Guide

## Overview

This document provides comprehensive testing guidelines for the multi-language support feature in the Azkary app. It covers manual testing scenarios, automated testing recommendations, and edge case handling.

## Supported Languages

- **English (en)** - Left-to-Right (LTR) language
- **Arabic (ar)** - Right-to-Left (RTL) language
- **System Default** - Follows device system language settings

## Test Environment Setup

### Prerequisites
1. Android device/emulator running API 21+ (Android 5.0+)
2. For Android 13+ testing: Device with per-app language settings support
3. Test devices in both RTL and LTR system language configurations

### Test Data Preparation
- Azkar content with mixed Arabic and English text
- Test dates and times for date/time formatting validation
- Test data with page counters, progress percentages, and verse references

---

## Manual Testing Checklist

### 1. Language Switching Tests

#### 1.1 English to Arabic Switch
- [ ] Navigate to Settings screen
- [ ] Tap on Language selector
- [ ] Select "Arabic" (العربية)
- [ ] Verify all UI text switches to Arabic
- [ ] Verify layout direction changes to RTL
- [ ] Verify navigation icons flip (back button, drawer, etc.)
- [ ] Verify text alignment changes to right-aligned
- [ ] Verify numeric displays use Arabic numerals where appropriate

#### 1.2 Arabic to English Switch
- [ ] Navigate to Settings screen
- [ ] Tap on Language selector
- [ ] Select "English"
- [ ] Verify all UI text switches to English
- [ ] Verify layout direction changes to LTR
- [ ] Verify navigation icons flip back
- [ ] Verify text alignment changes to left-aligned
- [ ] Verify numeric displays use Western numerals

#### 1.3 System Default Language
- [ ] Navigate to Settings screen
- [ ] Tap on Language selector
- [ ] Select "System Default"
- [ ] Verify app follows device system language
- [ ] Change device system language
- [ ] Verify app updates to match new system language

### 2. Language Persistence Tests

#### 2.1 Restart After Language Change
- [ ] Change language to Arabic
- [ ] Force close the app (swipe away from recent apps)
- [ ] Reopen the app
- [ ] Verify language remains Arabic
- [ ] Verify layout remains RTL

#### 2.2 Device Restart
- [ ] Change language to English
- [ ] Restart the device
- [ ] Reopen the app
- [ ] Verify language remains English
- [ ] Verify layout remains LTR

#### 2.3 App Update
- [ ] Set language to Arabic
- [ ] Install app update (if available)
- [ ] Verify language persists after update

### 3. RTL Layout Behavior Tests (Arabic)

#### 3.1 Reading Screen
- [ ] Open Reading screen in Arabic
- [ ] Verify azkar text displays right-to-left
- [ ] Verify page counter "(1/10)" displays correctly with proper number ordering
- [ ] Verify progress bar fills from right to left
- [ ] Verify repeat counter displays correctly
- [ ] Verify verse references display with proper bidi handling

#### 3.2 Settings Screen
- [ ] Open Settings screen in Arabic
- [ ] Verify all menu items are right-aligned
- [ ] Verify icons appear on the right side of text
- [ ] Verify checkboxes/radio buttons appear on the right
- [ ] Verify language selector displays correctly

#### 3.3 Summary Screen
- [ ] Open Summary screen in Arabic
- [ ] Verify statistics display right-aligned
- [ ] Verify progress indicators work correctly
- [ ] Verify date/time displays use Arabic numerals

#### 3.4 Navigation
- [ ] Verify back button appears on the right
- [ ] Verify bottom navigation bar icons are in correct order
- [ ] Verify drawer opens from the right (if applicable)
- [ ] Verify swipe gestures work in correct direction

### 4. LTR Layout Behavior Tests (English)

#### 4.1 Reading Screen
- [ ] Open Reading screen in English
- [ ] Verify azkar text displays left-to-right
- [ ] Verify page counter "(1/10)" displays correctly
- [ ] Verify progress bar fills from left to right
- [ ] Verify repeat counter displays correctly
- [ ] Verify verse references display correctly

#### 4.2 Settings Screen
- [ ] Open Settings screen in English
- [ ] Verify all menu items are left-aligned
- [ ] Verify icons appear on the left side of text
- [ ] Verify checkboxes/radio buttons appear on the left
- [ ] Verify language selector displays correctly

#### 4.3 Summary Screen
- [ ] Open Summary screen in English
- [ ] Verify statistics display left-aligned
- [ ] Verify progress indicators work correctly
- [ ] Verify date/time displays use Western numerals

#### 4.4 Navigation
- [ ] Verify back button appears on the left
- [ ] Verify bottom navigation bar icons are in correct order
- [ ] Verify drawer opens from the left (if applicable)
- [ ] Verify swipe gestures work in correct direction

### 5. Mixed Content Display Tests

#### 5.1 Arabic + Numbers
- [ ] Display azkar with Arabic text and numbers (e.g., "33 مرة")
- [ ] Verify numbers display with proper bidi handling
- [ ] Verify text flows correctly around numbers

#### 5.2 Arabic + Latin Characters
- [ ] Display verse references (e.g., "[Bukhari 5074]")
- [ ] Verify Latin characters display correctly
- [ ] Verify brackets and numbers maintain proper direction

#### 5.3 Complex Mixed Content
- [ ] Display content with Arabic, numbers, and Latin text
- [ ] Verify all elements display correctly
- [ ] Verify no text overlap or clipping

### 6. Date/Time Formatting Tests

#### 6.1 English Date Formatting
- [ ] Verify dates display in English format (e.g., "January 30, 2026")
- [ ] Verify month names are in English
- [ ] Verify Western numerals are used

#### 6.2 Arabic Date Formatting
- [ ] Verify dates display in Arabic format (e.g., "٣٠ يناير ٢٠٢٦")
- [ ] Verify month names are in Arabic
- [ ] Verify Arabic numerals are used

#### 6.3 Time Formatting
- [ ] Verify times display correctly in English (e.g., "3:45 PM")
- [ ] Verify times display correctly in Arabic (e.g., "٣:٤٥ م")
- [ ] Verify AM/PM indicators are localized

#### 6.4 Relative Time
- [ ] Verify "just now" / "الآن" displays correctly
- [ ] Verify "X minutes ago" / "منذ X دقيقة" displays correctly
- [ ] Verify "X hours ago" / "منذ X ساعة" displays correctly
- [ ] Verify "X days ago" / "منذ X يوم" displays correctly
- [ ] Verify future times display correctly

### 7. Page Counter and Progress Display Tests

#### 7.1 Page Counters
- [ ] Verify page counter "(1/10)" displays correctly in LTR
- [ ] Verify page counter "(1/10)" displays correctly in RTL
- [ ] Verify page counter maintains proper number ordering
- [ ] Test with single-digit and double-digit numbers

#### 7.2 Progress Percentages
- [ ] Verify progress "75%" displays correctly in LTR
- [ ] Verify progress "75%" displays correctly in RTL
- [ ] Verify progress "100%" displays correctly
- [ ] Verify progress "0%" displays correctly

#### 7.3 Repeat Counters
- [ ] Verify repeat counter "5/33" displays correctly in LTR
- [ ] Verify repeat counter "5/33" displays correctly in RTL
- [ ] Verify completion state "33/33" displays correctly

### 8. Settings Screen Language Selector Tests

#### 8.1 Language Options Display
- [ ] Verify language selector shows all options
- [ ] Verify option names are displayed in their respective languages
- [ ] Verify current language is highlighted/selected

#### 8.2 Language Selection
- [ ] Verify tapping a language option changes the language
- [ ] Verify UI updates immediately after selection
- [ ] Verify activity recreation occurs

#### 8.3 System Default Behavior
- [ ] Verify "System Default" option exists
- [ ] Verify selecting it follows device language
- [ ] Verify changing device language updates app

---

## Automated Testing

### Unit Tests

#### LocaleManager Tests
Location: `app/src/test/java/com/app/azkary/util/LocaleManagerTest.kt`

Test cases:
- Language change functionality
- Locale retrieval
- RTL detection
- Language tag retrieval
- Arabic language detection

Run with:
```bash
./gradlew test --tests LocaleManagerTest
```

#### BidiHelper Tests
Location: `app/src/test/java/com/app/azkary/util/BidiHelperTest.kt`

Test cases:
- Page counter formatting
- Progress formatting
- Repeat counter formatting
- Verse reference formatting
- Mixed text formatting
- LTR/RTL direction formatting

Run with:
```bash
./gradlew test --tests BidiHelperTest
```

#### DateTimeFormatter Tests
Location: `app/src/test/java/com/app/azkary/util/DateTimeFormatterTest.kt`

Test cases:
- Date formatting for English locale
- Date formatting for Arabic locale
- Time formatting
- Relative time formatting
- Timestamp formatting
- Date/time string parsing

Run with:
```bash
./gradlew test --tests DateTimeFormatterTest
```

### Instrumented Tests

#### Multi-Language UI Tests
Location: `app/src/androidTest/java/com/app/azkary/MultiLanguageUiTest.kt`

Test cases:
- Language switching in Settings screen
- Language persistence after activity recreation
- RTL/LTR layout direction verification
- String resource loading in both languages
- UI component direction verification

Run with:
```bash
./gradlew connectedAndroidTest --tests MultiLanguageUiTest
```

### Running All Tests

```bash
# Run all unit tests
./gradlew test

# Run all instrumented tests
./gradlew connectedAndroidTest

# Run tests with coverage
./gradlew test connectedAndroidTest jacocoTestReport
```

---

## Edge Cases and Special Scenarios

### 1. Android 13 Per-App Language Settings

#### Scenario: System Language vs App Language Conflict
- **Description**: User sets app language to Arabic while system is English (Android 13+)
- **Expected Behavior**: App uses Arabic, system uses English
- **Test Steps**:
  1. Set system language to English
  2. Open app and set language to Arabic
  3. Verify app displays in Arabic
  4. Verify system UI remains in English
  5. Check Android Settings > Apps > Azkary > Language
  6. Verify app language is set to Arabic

#### Scenario: Changing System Language on Android 13+
- **Description**: User changes system language after setting app language
- **Expected Behavior**: App maintains its independent language setting
- **Test Steps**:
  1. Set app language to Arabic
  2. Change system language to French
  3. Verify app remains in Arabic
  4. Verify system is in French

### 2. Missing Translations

#### Scenario: Missing String Resource in Arabic
- **Description**: A string exists in English but not in Arabic
- **Expected Behavior**: Falls back to English string
- **Test Steps**:
  1. Temporarily remove a string from `values-ar/strings.xml`
  2. Set language to Arabic
  3. Navigate to screen with missing string
  4. Verify English string is displayed
  5. Verify no crash occurs

#### Scenario: Missing Entire Translation File
- **Description**: Arabic strings.xml is missing or corrupted
- **Expected Behavior**: Falls back to English
- **Test Steps**:
  1. Temporarily rename `values-ar/strings.xml`
  2. Set language to Arabic
  3. Verify app displays in English
  4. Verify no crash occurs

### 3. OEM-Specific Locale Issues

#### Scenario: Custom ROM with Modified Locale Handling
- **Description**: Device with custom ROM that modifies locale behavior
- **Expected Behavior**: App should still function correctly
- **Test Steps**:
  1. Test on various OEM devices (Samsung, Xiaomi, etc.)
  2. Verify language switching works
  3. Verify RTL/LTR layout works
  4. Verify no crashes or unexpected behavior

#### Scenario: Region-Specific Arabic Variants
- **Description**: Device with regional Arabic variant (e.g., ar-SA, ar-EG)
- **Expected Behavior**: App uses base Arabic locale
- **Test Steps**:
  1. Set device to ar-SA (Saudi Arabia)
  2. Set app language to Arabic
  3. Verify Arabic strings are displayed
  4. Verify RTL layout is applied

### 4. App Restart Behavior

#### Scenario: Crash During Language Change
- **Description**: App crashes while changing language
- **Expected Behavior**: Language preference is saved, applied on next launch
- **Test Steps**:
  1. Set up app to crash during language change (simulated)
  2. Trigger language change
  3. Verify crash occurs
  4. Reopen app
  5. Verify language preference was saved
  6. Verify new language is applied

#### Scenario: Force Kill During Language Change
- **Description**: User force kills app during language change
- **Expected Behavior**: Language preference is saved
- **Test Steps**:
  1. Start language change
  2. Force kill app immediately
  3. Reopen app
  4. Verify language preference was saved
  5. Verify new language is applied

### 5. Different API Levels

#### API 21-25 (Android 5.0-7.1)
- **Test Focus**: Basic language switching, RTL support
- **Known Limitations**: No per-app language settings
- **Expected Behavior**: Language changes via app settings only

#### API 26-32 (Android 8.0-12L)
- **Test Focus**: Improved locale handling, better RTL support
- **Known Limitations**: No per-app language settings
- **Expected Behavior**: Language changes via app settings only

#### API 33+ (Android 13+)
- **Test Focus**: Per-app language settings integration
- **Expected Behavior**: App language syncs with system per-app settings

### 6. Memory and Performance

#### Scenario: Rapid Language Switching
- **Description**: User rapidly switches between languages
- **Expected Behavior**: No memory leaks, smooth transitions
- **Test Steps**:
  1. Switch between English and Arabic 10 times rapidly
  2. Monitor memory usage
  3. Verify no memory leaks
  4. Verify UI remains responsive

#### Scenario: Large Content with Language Change
- **Description**: Switch language while displaying large content
- **Expected Behavior**: Smooth transition, no lag
- **Test Steps**:
  1. Navigate to screen with long azkar content
  2. Switch language
  3. Verify smooth transition
  4. Verify no UI lag

### 7. Configuration Changes

#### Scenario: Screen Rotation During Language Change
- **Description**: User rotates device while language is changing
- **Expected Behavior**: Language change completes correctly
- **Test Steps**:
  1. Start language change
  2. Rotate device immediately
  3. Verify language is applied
  4. Verify layout is correct for new orientation

#### Scenario: Multi-Window Mode
- **Description**: App is in split-screen mode
- **Expected Behavior**: Language changes work correctly
- **Test Steps**:
  1. Open app in split-screen mode
  2. Change language
  3. Verify language changes
  4. Verify layout remains correct

### 8. Accessibility

#### Scenario: Screen Reader with RTL
- **Description**: Using TalkBack with Arabic language
- **Expected Behavior**: Text is read in correct direction
- **Test Steps**:
  1. Enable TalkBack
  2. Set language to Arabic
  3. Navigate through app
  4. Verify text is read right-to-left
  5. Verify numbers are read correctly

#### Scenario: Font Scaling
- **Description**: User has large font size enabled
- **Expected Behavior**: Text displays correctly in both languages
- **Test Steps**:
  1. Set font size to Large
  2. Switch to Arabic
  3. Verify text displays correctly
  4. Verify no text overflow

---

## Screenshot Testing

### Recommended Tools
- **Shot** - Screenshot testing library for Android
- **Paparazzi** - Compose screenshot testing
- **Roborazzi** - Robolectric screenshot testing

### Test Scenarios
1. Reading screen in English (LTR)
2. Reading screen in Arabic (RTL)
3. Settings screen language selector
4. Summary screen statistics
5. Mixed content displays
6. Page counters and progress indicators

### Example Shot Configuration
```kotlin
@Test
fun testReadingScreenEnglish() {
    val locale = Locale.ENGLISH
    val screenshot = screenshot(locale) {
        ReadingScreen(/* params */)
    }
    screenshot.assertAgainstGolden("reading_screen_en")
}

@Test
fun testReadingScreenArabic() {
    val locale = Locale("ar")
    val screenshot = screenshot(locale) {
        ReadingScreen(/* params */)
    }
    screenshot.assertAgainstGolden("reading_screen_ar")
}
```

---

## Continuous Integration

### GitHub Actions Example
```yaml
name: Multi-Language Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew test
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          script: ./gradlew connectedAndroidTest
```

---

## Bug Reporting Template

When reporting multi-language related bugs, include:

1. **Device Information**
   - Device model
   - Android version
   - API level
   - System language

2. **App State**
   - App language setting
   - Screen where bug occurred
   - Steps to reproduce

3. **Expected vs Actual Behavior**
   - What should happen
   - What actually happened

4. **Screenshots/Screen Recording**
   - Visual evidence of the issue

5. **Logs**
   - Relevant logcat output

---

## Test Coverage Goals

- **LocaleManager**: 90%+ coverage
- **BidiHelper**: 85%+ coverage
- **DateTimeFormatter**: 90%+ coverage
- **UI Components**: 70%+ coverage (instrumented tests)

---

## Conclusion

This testing guide provides comprehensive coverage for multi-language support in the Azkary app. Regular testing across different devices, API levels, and scenarios ensures a robust and user-friendly multilingual experience.

For questions or issues related to multi-language testing, refer to the main implementation plan at `plans/multi_language_implementation_plan.md`.
