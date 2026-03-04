package com.app.azkary.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.app.azkary.MainActivity

/**
 * Test utilities for UI tests
 */
object TestUtils {

    /**
     * Waits for a node with the given text to appear
     */
    fun ComposeTestRule.waitForNodeWithText(
        text: String,
        timeoutMillis: Long = 5000
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits for a node with the given content description to appear
     */
    fun ComposeTestRule.waitForNodeWithContentDescription(
        description: String,
        timeoutMillis: Long = 5000
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Performs a swipe gesture on a node
     */
    fun SemanticsNodeInteraction.swipeLeft() {
        performTouchInput {
            swipeLeft()
        }
    }

    /**
     * Performs a swipe right gesture on a node
     */
    fun SemanticsNodeInteraction.swipeRight() {
        performTouchInput {
            swipeRight()
        }
    }

    /**
     * Performs a swipe up gesture on a node
     */
    fun SemanticsNodeInteraction.swipeUp() {
        performTouchInput {
            swipeUp()
        }
    }

    /**
     * Performs a swipe down gesture on a node
     */
    fun SemanticsNodeInteraction.swipeDown() {
        performTouchInput {
            swipeDown()
        }
    }

    /**
     * Checks if a node has a progress bar range info
     */
    fun hasProgressBarRangeInfo(): SemanticsMatcher {
        return SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.ProgressBarRangeInfo)
    }

    /**
     * Common test tags for accessibility testing
     */
    object TestTags {
        const val SUMMARY_SCREEN = "summary_screen"
        const val READING_SCREEN = "reading_screen"
        const val SETTINGS_SCREEN = "settings_screen"
        const val CATEGORY_CREATION_SCREEN = "category_creation_screen"
        const val COMPLETION_SCREEN = "completion_screen"
        const val CATEGORY_LIST = "category_list"
        const val CATEGORY_ITEM = "category_item"
        const val READING_PAGER = "reading_pager"
        const val PROGRESS_INDICATOR = "progress_indicator"
        const val SAVE_BUTTON = "save_button"
        const val BACK_BUTTON = "back_button"
        const val ADD_BUTTON = "add_button"
        const val DELETE_BUTTON = "delete_button"
        const val EDIT_BUTTON = "edit_button"
        const val TOGGLE_BUTTON = "toggle_button"
    }

    /**
     * Common strings for testing
     */
    object TestStrings {
        const val SETTINGS = "Settings"
        const val BACK = "Back"
        const val SAVE = "Save"
        const val ADD = "Add"
        const val DELETE = "Delete"
        const val EDIT = "Edit"
        const val COMPLETE = "Complete"
        const val CANCEL = "Cancel"
        const val CONFIRM = "Confirm"
    }
}

/**
 * Extension function to scroll to a node if needed
 */
fun ComposeTestRule.scrollToNodeWithText(text: String) {
    onNodeWithText(text)
        .performScrollTo()
        .assertExists()
}

/**
 * Extension function to assert node is clickable
 */
fun SemanticsNodeInteraction.assertIsClickable(): SemanticsNodeInteraction {
    return assert(hasClickAction())
}