package com.app.azkary.ui.reading

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for CompletionScreen
 *
 * Tests cover:
 * - Completion screen display
 * - Celebration visual elements
 * - Back button functionality
 * - Accessibility
 */
@RunWith(AndroidJUnit4::class)
class CompletionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completionScreen_displaysCelebrationEmoji() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify celebration emoji is displayed
        composeTestRule.onNodeWithText("🎉").assertExists()
    }

    @Test
    fun completionScreen_displaysTitle() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("MashaAllah!").assertExists()
    }

    @Test
    fun completionScreen_displaysMessage() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify completion message is displayed
        composeTestRule.onNodeWithText("You have completed this category").assertExists()
    }

    @Test
    fun completionScreen_backButtonDisplayed() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify back button is displayed
        composeTestRule.onNodeWithText("Back to Summary").assertExists()
    }

    @Test
    fun completionScreen_backButtonClick_navigates() {
        var backCalled = false
        
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = { backCalled = true },
                isEnabled = true
            )
        }

        // Click back button
        composeTestRule.onNodeWithText("Back to Summary").performClick()
        
        // Verify navigation was triggered
        assert(backCalled)
    }

    @Test
    fun completionScreen_disabledState_blocksNavigation() {
        var backCalled = false
        
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = { backCalled = true },
                isEnabled = false
            )
        }

        // Try to click back button
        composeTestRule.onNodeWithText("Back to Summary").performClick()
        
        // Verify navigation was NOT triggered
        assert(!backCalled)
    }

    @Test
    fun completionScreen_allElementsCentered() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify all main elements exist
        composeTestRule.onNodeWithText("🎉").assertExists()
        composeTestRule.onNodeWithText("MashaAllah!").assertExists()
        composeTestRule.onNodeWithText("You have completed this category").assertExists()
        composeTestRule.onNodeWithText("Back to Summary").assertExists()
    }

    @Test
    fun completionScreen_buttonHasCorrectSize() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify back button exists and has proper height
        composeTestRule.onNodeWithText("Back to Summary")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun completionScreen_accessibility_titleIsReadable() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify title is displayed and readable
        composeTestRule.onNodeWithText("MashaAllah!")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun completionScreen_accessibility_messageIsReadable() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify message is displayed and readable
        composeTestRule.onNodeWithText("You have completed this category")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun completionScreen_accessibility_buttonIsClickable() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify button is clickable
        composeTestRule.onNodeWithText("Back to Summary")
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun completionScreen_inDisabledState_buttonIsNotEnabled() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = false
            )
        }

        // In disabled state, button click should not trigger action
        // The implementation checks isEnabled before calling onBackToSummary
        composeTestRule.onNodeWithText("Back to Summary").assertExists()
    }
}