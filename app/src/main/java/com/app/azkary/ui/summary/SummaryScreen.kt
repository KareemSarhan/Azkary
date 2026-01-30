package com.app.azkary.ui.summary

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val currentSession by viewModel.currentSession.collectAsState(initial = null)
    val sessionEndTime by viewModel.sessionEndTime.collectAsState(initial = null)

    val today = androidx.compose.runtime.remember {
        val currentLocale = Locale.getDefault()
        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(currentLocale))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineMedium)
                    Text(today, style = MaterialTheme.typography.bodyMedium)
                }
            }, actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.summary_settings_content_description))
                }
            })
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                currentSession?.let { session ->
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
                    CategoryItem(
                        category = category, onClick = { onNavigateToCategory(category.id) })
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
                        colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
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
                        color = Color(0xFF00E5FF),                 // bright ring like screenshot
                        trackColor = Color.White.copy(alpha = 0.25f) // faint ring behind
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItem(
    category: CategoryUi, onClick: () -> Unit
) {
    val progress = category.progress.coerceIn(0f, 1f)

    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.summary_scheduled), style = MaterialTheme.typography.bodySmall)
            }

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
