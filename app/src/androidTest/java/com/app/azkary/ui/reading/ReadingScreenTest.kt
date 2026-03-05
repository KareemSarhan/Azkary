package com.app.azkary.ui.reading

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.ui.ReadingScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for ReadingScreen and AzkarReadingItem
 *
 * Tests cover:
 * - Reading item display
 * - Increment interactions (click)
 * - Long press complete
 * - Completion screen display
 * - Navigation
 * - Accessibility
 */
@RunWith(AndroidJUnit4::class)
class ReadingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Test data
    private val mockItem = AzkarItemUi(
        id = "1",
        title = "Morning Dhikr",
        arabicText = "الحمد لله",
        transliteration = "Alhamdulillah",
        translation = "All praise is due to Allah",
        reference = "Bukhari",
        requiredRepeats = 3,
        currentRepeats = 1,
        isCompleted = false,
        isInfinite = false
    )

    private val mockInfiniteItem = AzkarItemUi(
        id = "2",
        title = "Tasbih",
        arabicText = "سبحان الله",
        transliteration = "Subhanallah",
        translation = "Glory be to Allah",
        reference = "Muslim",
        requiredRepeats = 0,
        currentRepeats = 10,
        isCompleted = false,
        isInfinite = true
    )

    private val mockCompletedItem = AzkarItemUi(
        id = "3",
        title = "Completed Dhikr",
        arabicText = "الله أكبر",
        transliteration = "Allahu Akbar",
        translation = "Allah is Greatest",
        reference = "Bukhari",
        requiredRepeats = 3,
        currentRepeats = 3,
        isCompleted = true,
        isInfinite = false
    )

    @Test
    fun azkarReadingItem_displaysAllContent() {
        composeTestRule.setContent {
            AzkarReadingItem(
                item = mockItem,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify all content is displayed
        composeTestRule.onNodeWithText("الحمد لله").assertExists()
        composeTestRule.onNodeWithText("Alhamdulillah").assertExists()
        composeTestRule.onNodeWithText("All praise is due to Allah").assertExists()
        composeTestRule.onNodeWithText("Bukhari").assertExists()
    }

    @Test
    fun azkarReadingItem_clickIncrementsCount() {
        var incrementCalled = false
        
        composeTestRule.setContent {
            AzkarReadingItem(
                item = mockItem,
                onIncrement = { incrementCalled = true },
                onHoldComplete = {}
            )
        }

        // Click on the item
        composeTestRule.onNodeWithText("الحمد لله").performClick()
        
        // Verify increment was called
        assert(incrementCalled)
    }

    @Test
    fun azkarReadingItem_longPressCompletes() {
        var completeCalled = false
        
        composeTestRule.setContent {
            AzkarReadingItem(
                item = mockItem,
                onIncrement = {},
                onHoldComplete = { completeCalled = true }
            )
        }

        // Long press on the item
        composeTestRule.onNodeWithText("الحمد لله").performTouchInput {
            longClick()
        }
        
        // Verify complete was called
        assert(completeCalled)
    }

    @Test
    fun azkarReadingItem_infiniteCounter() {
        composeTestRule.setContent {
            AzkarReadingItem(
                item = mockInfiniteItem,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify infinite item displays
        composeTestRule.onNodeWithText("سبحان الله").assertExists()
    }

    @Test
    fun azkarReadingItem_completedState() {
        composeTestRule.setContent {
            AzkarReadingItem(
                item = mockCompletedItem,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify completed item displays
        composeTestRule.onNodeWithText("الله أكبر").assertExists()
    }

    @Test
    fun azkarReadingItem_withoutTransliteration() {
        val itemWithoutTransliteration = mockItem.copy(transliteration = null)
        
        composeTestRule.setContent {
            AzkarReadingItem(
                item = itemWithoutTransliteration,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify Arabic text exists
        composeTestRule.onNodeWithText("الحمد لله").assertExists()
        // Verify transliteration section is not displayed
        composeTestRule.onNodeWithText("Transliteration").assertDoesNotExist()
    }

    @Test
    fun azkarReadingItem_withoutTranslation() {
        val itemWithoutTranslation = mockItem.copy(translation = null)
        
        composeTestRule.setContent {
            AzkarReadingItem(
                item = itemWithoutTranslation,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify Arabic text exists
        composeTestRule.onNodeWithText("الحمد لله").assertExists()
        // Verify translation section is not displayed
        composeTestRule.onNodeWithText("Translation").assertDoesNotExist()
    }

    @Test
    fun azkarReadingItem_withoutReference() {
        val itemWithoutReference = mockItem.copy(reference = null)
        
        composeTestRule.setContent {
            AzkarReadingItem(
                item = itemWithoutReference,
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify Arabic text exists
        composeTestRule.onNodeWithText("الحمد لله").assertExists()
        // Verify reference section is not displayed
        composeTestRule.onNodeWithText("Bukhari").assertDoesNotExist()
    }

    @Test
    fun completionScreen_displaysCorrectly() {
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = {},
                isEnabled = true
            )
        }

        // Verify completion screen content
        composeTestRule.onNodeWithText("MashaAllah!").assertExists()
        composeTestRule.onNodeWithText("You have completed this category").assertExists()
        composeTestRule.onNodeWithText("Back to Summary").assertExists()
    }

    @Test
    fun completionScreen_backButton_navigates() {
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
    fun completionScreen_disabledState() {
        var backCalled = false
        
        composeTestRule.setContent {
            CompletionScreen(
                onBackToSummary = { backCalled = true },
                isEnabled = false
            )
        }

        // Try to click back button when disabled
        composeTestRule.onNodeWithText("Back to Summary").performClick()
        
        // Verify navigation was not triggered
        assert(!backCalled)
    }

    @Test
    fun readingScreen_backButton_navigates() {
        var backCalled = false
        
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockItem),
                weightedProgress = 0.33f,
                onBack = { backCalled = true },
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Verify navigation was triggered
        assert(backCalled)
    }

    @Test
    fun readingScreen_progressBarDisplayed() {
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockItem),
                weightedProgress = 0.5f,
                onBack = {},
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify progress bar is displayed
        composeTestRule.onNode(hasProgressBarRangeInfo()).assertExists()
    }

    @Test
    fun readingScreen_pageCounterDisplayed() {
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockItem, mockInfiniteItem),
                weightedProgress = 0.5f,
                onBack = {},
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify page counter is displayed
        composeTestRule.onNodeWithText("1 of 2").assertExists()
    }

    @Test
    fun readingScreen_repeatCounterDisplayed() {
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockItem),
                weightedProgress = 0.33f,
                onBack = {},
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify repeat counter is displayed
        composeTestRule.onNodeWithText("1 / 3").assertExists()
    }

    @Test
    fun readingScreen_infiniteCounterDisplayed() {
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockInfiniteItem),
                weightedProgress = 0f,
                onBack = {},
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify infinite counter is displayed
        composeTestRule.onNodeWithText("10/∞").assertExists()
    }

    @Test
    fun readingScreen_accessibility_backButtonHasDescription() {
        composeTestRule.setContent {
            ReadingScreenContent(
                items = listOf(mockItem),
                weightedProgress = 0f,
                onBack = {},
                onIncrement = {},
                onHoldComplete = {}
            )
        }

        // Verify back button has content description
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }
}