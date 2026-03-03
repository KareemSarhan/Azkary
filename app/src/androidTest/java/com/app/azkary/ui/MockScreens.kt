package com.app.azkary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.model.AvailableZikr
import com.app.azkary.data.model.CategoryItemConfig
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.CategoryType
import com.app.azkary.data.prefs.LocationPreferences
import com.app.azkary.data.prefs.ThemeMode
import com.app.azkary.data.prefs.ThemeSettings

/**
 * Mock composables for UI testing
 * These are simplified versions of the actual screens for testing purposes
 */

@Composable
fun SummaryScreenContent(
    categories: List<CategoryUi>,
    currentSession: CategoryUi?,
    sessionEndTime: String?,
    isEditMode: Boolean,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCategory: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    onToggleEditMode: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onMoveCategoryUp: (Int) -> Unit,
    onMoveCategoryDown: (Int) -> Unit,
    onHoldComplete: (String) -> Unit,
    holdToComplete: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                actions = {
                    IconButton(
                        onClick = onToggleEditMode,
                        modifier = Modifier.semantics { contentDescription = "Edit" }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Settings" }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Current session card
            currentSession?.let { session ->
                item {
                    Card(
                        onClick = { onNavigateToCategory(session.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(session.name, style = MaterialTheme.typography.headlineSmall)
                            sessionEndTime?.let {
                                Text("Ends $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(
                                onClick = { onNavigateToCategory(session.id) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }

            // Categories
            if (categories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No categories for today")
                    }
                }
            } else {
                items(categories) { category ->
                    CategoryItemCard(
                        category = category,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode && category.type == CategoryType.USER) {
                                onNavigateToEditCategory(category.id)
                            } else {
                                onNavigateToCategory(category.id)
                            }
                        },
                        onLongClick = {
                            if (holdToComplete && !isEditMode) {
                                onHoldComplete(category.id)
                            }
                        },
                        onDelete = { onDeleteCategory(category.id) },
                        onMoveUp = { onMoveCategoryUp(categories.indexOf(category)) },
                        onMoveDown = { onMoveCategoryDown(categories.indexOf(category)) }
                    )
                }

                if (isEditMode) {
                    item {
                        Card(
                            onClick = onNavigateToCreateCategory,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Category")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItemCard(
    category: CategoryUi,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text("From ${category.from} to ${category.to}", style = MaterialTheme.typography.bodySmall)
            }

            if (isEditMode) {
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.semantics { contentDescription = "Move up" }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.semantics { contentDescription = "Move down" }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    if (category.type == CategoryType.USER) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics { contentDescription = "Delete" }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            } else {
                CircularProgressIndicator(
                    progress = { category.progress },
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ReadingScreenContent(
    items: List<AzkarItemUi>,
    weightedProgress: Float,
    onBack: () -> Unit,
    onIncrement: (String) -> Unit,
    onHoldComplete: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = { weightedProgress },
                modifier = Modifier.fillMaxWidth()
            )

            items.firstOrNull()?.let { item ->
                Text(
                    item.arabicText ?: "",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsScreenContent(
    locationPreferences: LocationPreferences,
    themeSettings: ThemeSettings,
    holdToComplete: Boolean,
    currentLanguageName: String,
    isRefreshingLocation: Boolean,
    locationError: String?,
    onBack: () -> Unit,
    onToggleUseLocation: (Boolean) -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHoldToComplete: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("PRAYER TIMES", style = MaterialTheme.typography.labelSmall)

            // Location toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use location for prayer times")
                Switch(
                    checked = locationPreferences.useLocation,
                    onCheckedChange = onToggleUseLocation
                )
            }

            if (locationPreferences.useLocation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Location")
                    if (isRefreshingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.semantics { contentDescription = "Refresh location" }
                        ) {
                            Text("Refresh")
                        }
                    }
                }
                locationPreferences.locationName?.let {
                    Text(it)
                }
            }

            Text("GENERAL", style = MaterialTheme.typography.labelSmall)

            // Language
            Card(
                onClick = onOpenLanguageSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Language")
                    Text(currentLanguageName)
                }
            }

            // Hold to complete toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hold to complete")
                Switch(
                    checked = holdToComplete,
                    onCheckedChange = onSetHoldToComplete
                )
            }

            Text("THEME", style = MaterialTheme.typography.labelSmall)

            // Theme options
            ThemeMode.values().forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeSettings.themeMode == mode,
                        onClick = { onSetThemeMode(mode) }
                    )
                    Text(mode.name)
                }
            }
        }
    }
}

@Composable
fun CategoryCreationScreenContent(
    categoryName: String,
    searchQuery: String,
    selectedItems: List<CategoryItemConfig>,
    availableItems: List<AvailableZikr>,
    isStockCategory: Boolean,
    from: Int,
    to: Int,
    isEditMode: Boolean = false,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCategoryNameChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onItemSelect: (String) -> Unit,
    onItemRemove: (String) -> Unit,
    onItemCountChange: (String, Int) -> Unit,
    onItemInfiniteToggle: (String) -> Unit,
    onFromChange: (Int) -> Unit,
    onToChange: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Category" else "Create Category") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.semantics { contentDescription = "Save" }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedItems.isNotEmpty()) {
                item {
                    Text("Selected Items", style = MaterialTheme.typography.titleMedium)
                }

                items(selectedItems) { config ->
                    val item = availableItems.find { it.id == config.itemId }
                    item?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(it.title ?: "")
                                    Text(it.arabicText?.take(20) ?: "")
                                }

                                Row {
                                    IconButton(
                                        onClick = { onItemCountChange(config.itemId, config.requiredRepeats - 1) },
                                        modifier = Modifier.semantics { contentDescription = "Decrease" }
                                    ) {
                                        Text("-")
                                    }
                                    Text(config.requiredRepeats.toString())
                                    IconButton(
                                        onClick = { onItemCountChange(config.itemId, config.requiredRepeats + 1) },
                                        modifier = Modifier.semantics { contentDescription = "Increase" }
                                    ) {
                                        Text("+")
                                    }
                                    IconButton(
                                        onClick = { onItemRemove(config.itemId) },
                                        modifier = Modifier.semantics { contentDescription = "Remove" }
                                    ) {
                                        Text("X")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (availableItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    item {
                        Text("Loading available zikr...")
                    }
                }
            } else {
                item {
                    Text("Choose Zikr", style = MaterialTheme.typography.titleMedium)
                }

                items(availableItems.filter { item ->
                    selectedItems.none { it.itemId == item.id }
                }) { item ->
                    Card(
                        onClick = { onItemSelect(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title ?: "")
                                Text(item.arabicText?.take(30) ?: "")
                            }
                            IconButton(
                                onClick = { onItemSelect(item.id) },
                                modifier = Modifier.semantics { contentDescription = "Add" }
                            ) {
                                Text("+")
                            }
                        }
                    }
                }
            }
        }
    }
}