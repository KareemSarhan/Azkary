package com.app.azkary.ui.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.azkary.R
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.ui.theme.SessionGradientEnd
import com.app.azkary.ui.theme.SessionGradientStart
import com.app.azkary.ui.theme.SessionRingColor
import com.app.azkary.util.BidiHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateCategory: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    LocalContext.current
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val currentSession by viewModel.currentSession.collectAsState(initial = null)
    val sessionEndTime by viewModel.sessionEndTime.collectAsState(initial = null)
    val isEditMode by viewModel.isEditMode.collectAsState()
    val holdToComplete by viewModel.holdToComplete.collectAsState(initial = true)
    val showWeeklyProgress by viewModel.showWeeklyProgress.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState(initial = emptyList())

    val today = androidx.compose.runtime.remember {
        val currentLocale = Locale.getDefault()
        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(currentLocale))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(today, style = MaterialTheme.typography.bodyMedium)
                }
            }, actions = {
                IconButton(onClick = { viewModel.toggleEditMode() }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.summary_settings_content_description))
                }
            })
        },
        bottomBar = {
            if (isEditMode) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Button(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.category_complete_editing))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentSession?.let { session ->
                item {
                    CurrentSessionCard(
                        category = session,
                        sessionEndTime = sessionEndTime,
                        onContinue = { onNavigateToCategory(session.id) })
                }
            }

            item {
                Text(
                    text = stringResource(R.string.summary_today),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (categories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.summary_no_categories),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items = categories, key = { it.id } // stable key
                ) { category ->
                    val index = categories.indexOf(category)
                    CategoryItem(
                        category = category,
                        index = index,
                        totalCount = categories.size,
                        isEditMode = isEditMode,
                        onClick = { onNavigateToCategory(category.id) },
                        onEdit = { onNavigateToEditCategory(category.id) },
                        onDelete = { viewModel.deleteCategory(category.id) },
                        onMoveUp = { if (index > 0) viewModel.moveCategoryUp(index) },
                        onMoveDown = { if (index < categories.size - 1) viewModel.moveCategoryDown(index) },
                        holdToComplete = holdToComplete,
                        onHoldComplete = { viewModel.toggleCategoryCompletion(category.id) }
                    )
                }

                if (isEditMode) {
                    item {
                        AddCategoryItem(
                            onClick = onNavigateToCreateCategory
                        )
                    }
                }

                // Weekly Progress Card at bottom
                if (showWeeklyProgress && weeklyProgress.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyProgressCard(days = weeklyProgress)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentSessionCard(
    category: CategoryUi,
    sessionEndTime: String?,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val progress = category.progress.coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()                 // ✅ important (fixes cut-off look)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SessionGradientStart, SessionGradientEnd)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(category.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = sessionEndTime?.let { stringResource(R.string.summary_session_ends, it) } ?: stringResource(R.string.summary_session_missed),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = BidiHelper.formatProgress((progress * 100).toInt(), context),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(stringResource(R.string.summary_continue))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    RingProgress(
                        progress = progress,
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 8.dp,
                        color = SessionRingColor,
                        trackColor = Color.White.copy(alpha = 0.25f) // faint ring behind
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryItem(
    category: CategoryUi,
    index: Int,
    totalCount: Int,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    holdToComplete: Boolean = true,
    onHoldComplete: () -> Unit = {}
) {
    val progress = category.progress.coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditMode && category.type == com.app.azkary.data.model.CategoryType.DEFAULT) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        onClick = {
                            if (isEditMode) onEdit() else onClick()
                        },
                        onLongClick = {
                            if (!isEditMode && holdToComplete) onHoldComplete()
                        }
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(formatScheduleText(category.from, category.to), style = MaterialTheme.typography.bodySmall)
            }

            if (isEditMode) {
                when (category.type) {
                    com.app.azkary.data.model.CategoryType.USER -> {
                        Row {
                            IconButton(
                                onClick = onMoveUp,
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            }
                            IconButton(
                                onClick = onMoveDown,
                                enabled = index < totalCount - 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                    com.app.azkary.data.model.CategoryType.DEFAULT -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "System category - read only",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                RingProgress(
                    progress = progress,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            }

        }
    }
}

@Composable
private fun formatScheduleText(from: Int, to: Int): String {
    val fromName = when (from) {
        0 -> stringResource(R.string.schedule_fajr)
        1 -> stringResource(R.string.schedule_sunrise)
        2 -> stringResource(R.string.schedule_dhuhr)
        3 -> stringResource(R.string.schedule_asr)
        4 -> stringResource(R.string.schedule_maghrib)
        5 -> stringResource(R.string.schedule_isha)
        6 -> stringResource(R.string.schedule_firstthird)
        7 -> stringResource(R.string.schedule_midnight)
        8 -> stringResource(R.string.schedule_lastthird)
        else -> from.toString()
    }
    
    val toName = when (to) {
        0 -> stringResource(R.string.schedule_fajr)
        1 -> stringResource(R.string.schedule_sunrise)
        2 -> stringResource(R.string.schedule_dhuhr)
        3 -> stringResource(R.string.schedule_asr)
        4 -> stringResource(R.string.schedule_maghrib)
        5 -> stringResource(R.string.schedule_isha)
        6 -> stringResource(R.string.schedule_firstthird)
        7 -> stringResource(R.string.schedule_midnight)
        8 -> stringResource(R.string.schedule_lastthird)
        else -> to.toString()
    }
    
    val isNextDay = to < from
    val fromLabel = stringResource(R.string.schedule_from)
    val toLabel = stringResource(R.string.schedule_to)
    val nextDaySuffix = stringResource(R.string.schedule_next_day_suffix)
    
    return if (isNextDay) {
        "$fromLabel $fromName $toLabel $toName $nextDaySuffix"
    } else {
        "$fromLabel $fromName $toLabel $toName"
    }
}

@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp,
    color: Color,
    trackColor: Color
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Track ring (full circle)
        CircularProgressIndicator(
            progress = { 1f },
            color = trackColor,
            strokeWidth = strokeWidth,
            modifier = Modifier.matchParentSize()
        )

        // Progress arc
        CircularProgressIndicator(
            progress = { clampedProgress },
            color = color,
            strokeWidth = strokeWidth,
            modifier = Modifier.matchParentSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryItem(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.summary_add_category),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
