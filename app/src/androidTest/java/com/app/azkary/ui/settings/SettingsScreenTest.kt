package com.app.azkary.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.azkary.data.model.LatLng
import com.app.azkary.data.prefs.LocationPreferences
import com.app.azkary.data.prefs.ThemeMode
import com.app.azkary.data.prefs.ThemeSettings
import com.app.azkary.ui.SettingsScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for SettingsScreen
 *
 * Tests cover:
 * - Settings display
 * - Toggle interactions
 * - Theme selection
 * - Language settings
 * - Location preferences
 * - Accessibility
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun settingsScreen_backButton_navigates() {
        var backCalled = false
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = { backCalled = true },
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Verify navigation was triggered
        assert(backCalled)
    }

    @Test
    fun settingsScreen_locationToggle_toggles() {
        var locationEnabled = false
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(useLocation = locationEnabled),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = { locationEnabled = it },
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Find and click the location toggle
        composeTestRule.onNodeWithText("Use location for prayer times").performClick()
        
        // Verify toggle was triggered
        assert(locationEnabled)
    }

    @Test
    fun settingsScreen_languageButton_opensSettings() {
        var languageSettingsOpened = false
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = { languageSettingsOpened = true },
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Click language button
        composeTestRule.onNodeWithText("Language").performClick()
        
        // Verify language settings was opened
        assert(languageSettingsOpened)
    }

    @Test
    fun settingsScreen_displaysCurrentLanguage() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "العربية",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify current language is displayed
        composeTestRule.onNodeWithText("العربية").assertExists()
    }

    @Test
    fun settingsScreen_themeOptionsDisplayed() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(themeMode = ThemeMode.SYSTEM),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify theme options are displayed
        composeTestRule.onNodeWithText("SYSTEM").assertExists()
        composeTestRule.onNodeWithText("LIGHT").assertExists()
        composeTestRule.onNodeWithText("DARK").assertExists()
    }

    @Test
    fun settingsScreen_themeSelection_changesTheme() {
        var selectedTheme: ThemeMode? = null
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(themeMode = ThemeMode.SYSTEM),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = { selectedTheme = it },
                onSetHoldToComplete = {}
            )
        }

        // Click on DARK theme option
        composeTestRule.onNodeWithText("DARK").performClick()
        
        // Verify DARK theme was selected
        assert(selectedTheme == ThemeMode.DARK)
    }

    @Test
    fun settingsScreen_holdToCompleteToggle_toggles() {
        var holdToComplete = true
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = holdToComplete,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = { holdToComplete = it }
            )
        }

        // Click on Hold to complete toggle
        composeTestRule.onNodeWithText("Hold to complete").performClick()
        
        // Verify toggle was triggered (set to false)
        assert(!holdToComplete)
    }

    @Test
    fun settingsScreen_locationEnabled_showsLocationDetails() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(
                    useLocation = true,
                    lastResolvedLocation = LatLng(24.7136, 46.6753),
                    locationName = "Riyadh"
                ),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify location details are displayed
        composeTestRule.onNodeWithText("Current Location").assertExists()
        composeTestRule.onNodeWithText("Riyadh").assertExists()
    }

    @Test
    fun settingsScreen_refreshLocationButton_triggersRefresh() {
        var refreshCalled = false
        
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(useLocation = true),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = { refreshCalled = true },
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Click refresh location button
        composeTestRule.onNodeWithContentDescription("Refresh location").performClick()
        
        // Verify refresh was triggered
        assert(refreshCalled)
    }

    @Test
    fun settingsScreen_sectionsDisplayed() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify section headers are displayed
        composeTestRule.onNodeWithText("PRAYER TIMES").assertExists()
        composeTestRule.onNodeWithText("GENERAL").assertExists()
        composeTestRule.onNodeWithText("THEME").assertExists()
    }

    @Test
    fun settingsScreen_accessibility_backButtonHasDescription() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = false,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify back button has content description
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun settingsScreen_locationLoading_showsProgress() {
        composeTestRule.setContent {
            SettingsScreenContent(
                locationPreferences = LocationPreferences(useLocation = true),
                themeSettings = ThemeSettings(),
                holdToComplete = true,
                currentLanguageName = "English",
                isRefreshingLocation = true,
                locationError = null,
                onBack = {},
                onToggleUseLocation = {},
                onRefreshLocation = {},
                onOpenLanguageSettings = {},
                onSetThemeMode = {},
                onSetHoldToComplete = {}
            )
        }

        // Verify progress indicator is shown (circular progress indicator)
        composeTestRule.onNode(hasProgressBarRangeInfo()).assertExists()
    }
}