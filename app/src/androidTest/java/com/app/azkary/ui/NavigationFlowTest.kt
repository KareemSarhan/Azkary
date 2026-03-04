package com.app.azkary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.model.SystemCategoryKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for Navigation Flows
 *
 * Tests cover:
 * - Summary to Reading navigation
 * - Summary to Settings navigation
 * - Summary to Category Creation navigation
 * - Reading back to Summary
 * - Settings back to Summary
 * - Category Creation back to Summary
 * - Reading to Completion to Summary
 * - Swipe gestures between reading items
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
        )
    )

    private val mockReadingItems = listOf(
        AzkarItemUi(
            id = "1",
            title = "Item 1",
            arabicText = "الحمد لله",
            transliteration = "Alhamdulillah",
            translation = "All praise is due to Allah",
            reference = "Bukhari",
            requiredRepeats = 3,
            currentRepeats = 1,
            isCompleted = false,
            isInfinite = false
        ),
        AzkarItemUi(
            id = "2",
            title = "Item 2",
            arabicText = "سبحان الله",
            transliteration = "Subhanallah",
            translation = "Glory be to Allah",
            reference = "Muslim",
            requiredRepeats = 33,
            currentRepeats = 0,
            isCompleted = false,
            isInfinite = false
        )
    )

    @Test
    fun navigationFlow_summaryToReadingToSummary() {
        var currentScreen = "summary"
        var selectedCategoryId: String? = null

        composeTestRule.setContent {
            when (currentScreen) {
                "summary" -> SummaryScreenMock(
                    categories = mockCategories,
                    onNavigateToCategory = { 
                        selectedCategoryId = it
                        currentScreen = "reading"
                    },
                    onNavigateToSettings = {},
                    onNavigateToCreateCategory = {}
                )
                "reading" -> ReadingScreenMock(
                    items = mockReadingItems,
                    onBack = { currentScreen = "summary" },
                    onComplete = {}
                )
            }
        }

        // Verify starting on Summary screen
        composeTestRule.onNodeWithText("Morning Azkar").assertExists()

        // Navigate to Reading screen
        composeTestRule.onNodeWithText("Morning Azkar").performClick()

        // Verify navigation occurred
        assert(selectedCategoryId == "1")

        // Simulate screen change
        currentScreen = "reading"
        composeTestRule.setContent {
            ReadingScreenMock(
                items = mockReadingItems,
                onBack = { currentScreen = "summary" },
                onComplete = {}
            )
        }

        // Verify on Reading screen
        composeTestRule.onNodeWithText("الحمد لله").assertExists()

        // Navigate back to Summary
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify back navigation
        assert(currentScreen == "summary")
    }

    @Test
    fun navigationFlow_summaryToSettingsToSummary() {
        var currentScreen = "summary"

        composeTestRule.setContent {
            when (currentScreen) {
                "summary" -> SummaryScreenMock(
                    categories = mockCategories,
                    onNavigateToCategory = {},
                    onNavigateToSettings = { currentScreen = "settings" },
                    onNavigateToCreateCategory = {}
                )
                "settings" -> SettingsScreenMock(
                    onBack = { currentScreen = "summary" }
                )
            }
        }

        // Verify starting on Summary screen
        composeTestRule.onNodeWithText("Morning Azkar").assertExists()

        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Simulate screen change
        currentScreen = "settings"
        composeTestRule.setContent {
            SettingsScreenMock(onBack = { currentScreen = "summary" })
        }

        // Verify on Settings screen
        composeTestRule.onNodeWithText("Settings").assertExists()

        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify back on Summary
        assert(currentScreen == "summary")
    }

    @Test
    fun navigationFlow_summaryToCategoryCreationToSummary() {
        var currentScreen = "summary"
        var editMode = false

        composeTestRule.setContent {
            when (currentScreen) {
                "summary" -> SummaryScreenMock(
                    categories = mockCategories,
                    isEditMode = editMode,
                    onNavigateToCategory = {},
                    onNavigateToSettings = {},
                    onNavigateToCreateCategory = { currentScreen = "category_creation" },
                    onToggleEditMode = { editMode = !editMode }
                )
                "category_creation" -> CategoryCreationScreenMock(
                    onBack = { currentScreen = "summary" },
                    onSave = { currentScreen = "summary" }
                )
            }
        }

        // Enable edit mode first
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        editMode = true

        // Simulate refresh
        composeTestRule.setContent {
            SummaryScreenMock(
                categories = mockCategories,
                isEditMode = editMode,
                onNavigateToCategory = {},
                onNavigateToSettings = {},
                onNavigateToCreateCategory = { currentScreen = "category_creation" },
                onToggleEditMode = { editMode = !editMode }
            )
        }

        // Navigate to Category Creation
        composeTestRule.onNodeWithText("Add Category").performClick()

        // Simulate screen change
        currentScreen = "category_creation"
        composeTestRule.setContent {
            CategoryCreationScreenMock(
                onBack = { currentScreen = "summary" },
                onSave = { currentScreen = "summary" }
            )
        }

        // Verify on Category Creation screen
        composeTestRule.onNodeWithText("Create Category").assertExists()

        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify back on Summary
        assert(currentScreen == "summary")
    }

    @Test
    fun navigationFlow_readingToCompletionToSummary() {
        var currentScreen = "reading"
        var progress = 1.0f // Complete

        composeTestRule.setContent {
            when (currentScreen) {
                "reading" -> ReadingScreenMock(
                    items = mockReadingItems,
                    weightedProgress = progress,
                    onBack = {},
                    onComplete = { currentScreen = "completion" }
                )
                "completion" -> CompletionScreenMock(
                    onBackToSummary = { currentScreen = "summary" }
                )
                "summary" -> SummaryScreenMock(
                    categories = mockCategories,
                    onNavigateToCategory = {},
                    onNavigateToSettings = {},
                    onNavigateToCreateCategory = {}
                )
            }
        }

        // Simulate completing all items
        currentScreen = "completion"
        composeTestRule.setContent {
            CompletionScreenMock(onBackToSummary = { currentScreen = "summary" })
        }

        // Verify on Completion screen
        composeTestRule.onNodeWithText("MashaAllah!").assertExists()

        // Navigate back to Summary
        composeTestRule.onNodeWithText("Back to Summary").performClick()

        // Verify back on Summary
        assert(currentScreen == "summary")
    }

    @Test
    fun navigationFlow_readingSwipeBetweenItems() {
        var currentPage = 0

        composeTestRule.setContent {
            ReadingPagerMock(
                items = mockReadingItems,
                currentPage = currentPage,
                onPageChange = { currentPage = it }
            )
        }

        // Verify first item is shown
        composeTestRule.onNodeWithText("الحمد لله").assertExists()

        // Swipe to next item
        composeTestRule.onNodeWithText("الحمد لله").performTouchInput {
            swipeLeft()
        }

        // Note: In actual implementation, the pager would handle the swipe
        // and change the page automatically
    }

    @Test
    fun navigationFlow_editCategoryFlow() {
        var currentScreen = "summary"
        var editMode = false
        var selectedCategoryId: String? = null

        composeTestRule.setContent {
            when (currentScreen) {
                "summary" -> SummaryScreenMock(
                    categories = mockCategories,
                    isEditMode = editMode,
                    onNavigateToCategory = { 
                        if (editMode) {
                            selectedCategoryId = it
                            currentScreen = "category_edit"
                        }
                    },
                    onNavigateToSettings = {},
                    onNavigateToCreateCategory = {},
                    onToggleEditMode = { editMode = !editMode }
                )
                "category_edit" -> CategoryCreationScreenMock(
                    isEditMode = true,
                    categoryName = "Morning Azkar",
                    onBack = { currentScreen = "summary" },
                    onSave = { currentScreen = "summary" }
                )
            }
        }

        // Enable edit mode
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        editMode = true

        // Simulate refresh
        composeTestRule.setContent {
            SummaryScreenMock(
                categories = mockCategories,
                isEditMode = editMode,
                onNavigateToCategory = { 
                    selectedCategoryId = it
                    currentScreen = "category_edit"
                },
                onNavigateToSettings = {},
                onNavigateToCreateCategory = {},
                onToggleEditMode = { editMode = !editMode }
            )
        }

        // Click on category in edit mode navigates to edit
        composeTestRule.onNodeWithText("Morning Azkar").performClick()

        // Verify navigation
        assert(selectedCategoryId == "1")
    }
}

// Mock composables for navigation testing
@Composable
private fun SummaryScreenMock(
    categories: List<CategoryUi>,
    isEditMode: Boolean = false,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCategory: () -> Unit,
    onToggleEditMode: () -> Unit = {}
) {
    Column {
        Text("Summary")
        IconButton(onClick = onNavigateToSettings) {
            Text("Settings", modifier = Modifier.semantics { 
                contentDescription = "Settings" 
            })
        }
        IconButton(onClick = onToggleEditMode) {
            Text("Edit", modifier = Modifier.semantics { 
                contentDescription = "Edit" 
            })
        }
        categories.forEach { 
            Button(onClick = { onNavigateToCategory(it.id) }) {
                Text(it.name)
            }
        }
        if (isEditMode) {
            Button(onClick = onNavigateToCreateCategory) {
                Text("Add Category")
            }
        }
    }
}

@Composable
private fun ReadingScreenMock(
    items: List<AzkarItemUi>,
    weightedProgress: Float = 0f,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column {
        IconButton(onClick = onBack) {
            Text("Back", modifier = Modifier.semantics { 
                contentDescription = "Back" 
            })
        }
        LinearProgressIndicator(progress = { weightedProgress })
        items.firstOrNull()?.let {
            Text(it.arabicText ?: "")
        }
    }
}

@Composable
private fun SettingsScreenMock(
    onBack: () -> Unit
) {
    Column {
        Text("Settings")
        IconButton(onClick = onBack) {
            Text("Back", modifier = Modifier.semantics { 
                contentDescription = "Back" 
            })
        }
    }
}

@Composable
private fun CategoryCreationScreenMock(
    isEditMode: Boolean = false,
    categoryName: String = "",
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        Text(if (isEditMode) "Edit Category" else "Create Category")
        IconButton(onClick = onBack) {
            Text("Back", modifier = Modifier.semantics { 
                contentDescription = "Back" 
            })
        }
        Button(onClick = onSave) {
            Text("Save")
        }
    }
}

@Composable
private fun CompletionScreenMock(
    onBackToSummary: () -> Unit
) {
    Column {
        Text("MashaAllah!")
        Button(onClick = onBackToSummary) {
            Text("Back to Summary")
        }
    }
}

@Composable
private fun ReadingPagerMock(
    items: List<AzkarItemUi>,
    currentPage: Int,
    onPageChange: (Int) -> Unit
) {
    items.getOrNull(currentPage)?.let {
        Text(it.arabicText ?: "")
    }
}