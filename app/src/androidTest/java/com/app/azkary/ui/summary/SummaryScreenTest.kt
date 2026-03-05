package com.app.azkary.ui.summary

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for SummaryScreen
 *
 * Tests cover:
 * - Screen rendering with categories
 * - Empty state display
 * - Edit mode toggle
 * - Category card interactions (click, long press)
 * - Navigation actions
 * - Accessibility
 */
@RunWith(AndroidJUnit4::class)
class SummaryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Test data
    private val mockCategories = listOf(
        CategoryUi(
            id = "1",
            name = "Morning Azkar",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.MORNING,
            progress = 0.5f,
            from = 0,
            to = 2
        ),
        CategoryUi(
            id = "2",
            name = "Evening Azkar",
            type = CategoryType.DEFAULT,
            systemKey = SystemCategoryKey.EVENING,
            progress = 0.0f,
            from = 4,
            to = 5
        ),
        CategoryUi(
            id = "3",
            name = "Custom Category",
            type = CategoryType.USER,
            systemKey = null,
            progress = 1.0f,
            from = 0,
            to = 8
        )
    )

    @Test
    fun summaryScreen_displaysHeaderAndCategories() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = mockCategories.first(),
                sessionEndTime = "8:30 AM",
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify header elements exist
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
        
        // Verify categories are displayed
        composeTestRule.onNodeWithText("Morning Azkar").assertExists()
        composeTestRule.onNodeWithText("Evening Azkar").assertExists()
        composeTestRule.onNodeWithText("Custom Category").assertExists()
    }

    @Test
    fun summaryScreen_displaysEmptyState() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = emptyList(),
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify empty state message
        composeTestRule.onNodeWithText("No categories for today").assertExists()
    }

    @Test
    fun summaryScreen_editModeToggle() {
        var editMode = false
        
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = editMode,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = { editMode = !editMode },
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Click edit button
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        
        // Verify edit mode is triggered
        assert(editMode)
    }

    @Test
    fun summaryScreen_categoryClick_navigatesToCategory() {
        var clickedCategoryId: String? = null
        
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = { clickedCategoryId = it },
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Click on a category
        composeTestRule.onNodeWithText("Morning Azkar").performClick()
        
        // Verify navigation was triggered
        assert(clickedCategoryId == "1")
    }

    @Test
    fun summaryScreen_longPress_togglesCompletion() {
        var completedCategoryId: String? = null
        
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = { completedCategoryId = it },
                holdToComplete = true
            )
        }

        // Long press on a category
        composeTestRule.onNodeWithText("Morning Azkar").performTouchInput {
            longClick()
        }
        
        // Verify completion toggle was triggered
        assert(completedCategoryId == "1")
    }

    @Test
    fun summaryScreen_settingsButton_navigatesToSettings() {
        var navigatedToSettings = false
        
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = { navigatedToSettings = true },
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Click settings button
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Verify navigation was triggered
        assert(navigatedToSettings)
    }

    @Test
    fun summaryScreen_currentSessionCard_displayed() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = mockCategories.first(),
                sessionEndTime = "8:30 AM",
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify current session card is displayed
        composeTestRule.onNodeWithText("Morning Azkar").assertExists()
        composeTestRule.onNodeWithText("Continue").assertExists()
    }

    @Test
    fun summaryScreen_editMode_showsReorderAndDeleteButtons() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = true,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify reorder buttons exist in edit mode (using semantics)
        composeTestRule.onAllNodesWithContentDescription("Move up").assertCountEquals(3)
        composeTestRule.onAllNodesWithContentDescription("Move down").assertCountEquals(3)
    }

    @Test
    fun summaryScreen_accessibility_labelsPresent() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify content descriptions exist
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun summaryScreen_addCategoryButton_inEditMode() {
        var createCategoryClicked = false
        
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = true,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = { createCategoryClicked = true },
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Click add category button
        composeTestRule.onNodeWithText("Add Category").performClick()
        
        // Verify navigation was triggered
        assert(createCategoryClicked)
    }

    @Test
    fun summaryScreen_progressIndicatorsDisplayed() {
        composeTestRule.setContent {
            SummaryScreenContent(
                categories = mockCategories,
                currentSession = null,
                sessionEndTime = null,
                isEditMode = false,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onNavigateToEditCategory = {},
                onToggleEditMode = {},
                onDeleteCategory = {},
                onMoveCategoryUp = {},
                onMoveCategoryDown = {},
                onHoldComplete = {},
                holdToComplete = true
            )
        }

        // Verify progress indicators are displayed (by checking circular progress indicators)
        composeTestRule.onAllNodes(hasProgressBarRangeInfo()).assertCountEquals(3)
    }
}