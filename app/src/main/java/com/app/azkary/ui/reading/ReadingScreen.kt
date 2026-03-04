package com.app.azkary.ui.reading

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.azkary.R
import com.app.azkary.util.BidiHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    onBack: () -> Unit,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState(initial = emptyList())
    val weightedProgress by viewModel.weightedProgress.collectAsState(initial = 0f)
    val holdToComplete by viewModel.holdToComplete.collectAsState(initial = true)
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState(initial = true)

    // If categoryId is null, navigate back immediately
    LaunchedEffect(Unit) {
        if (viewModel.categoryId == null) {
            onBack()
        }
    }

    var isActive by remember { mutableStateOf(true) }

    // Safe navigation wrapper that prevents double-navigation by immediately disabling
    val safeOnBack: () -> Unit = {
        if (isActive) {
            isActive = false
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isActive = false
        }
    }

    BackHandler(enabled = isActive) {
        safeOnBack()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = weightedProgress.coerceIn(0f, 1f),
        label = "weightedProgressAnimation"
    )

    val isComplete = weightedProgress >= 1f
    val pagerState = rememberPagerState(pageCount = { if (isComplete) items.size + 1 else items.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val initialPageIndex = remember(items, isComplete) {
        if (isComplete) {
            items.size  // Completion page
        } else {
            items.indexOfFirst { !it.isInfinite && it.currentRepeats < it.requiredRepeats }.takeIf { it >= 0 } ?: 0
        }
    }

    LaunchedEffect(initialPageIndex) {
        if (pagerState.currentPage != initialPageIndex && items.isNotEmpty()) {
            pagerState.scrollToPage(initialPageIndex)
        }
    }

    // Initialize Vibrator
    val vibrator = remember {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    }

    // Helper to trigger vibration (respects user preference)
    val performVibration: (Long) -> Unit = { duration ->
        if (vibrationEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    val colors = MaterialTheme.colorScheme
    val holdToCompleteDisabledMessage = stringResource(R.string.hold_to_complete_disabled)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.surface,
        topBar = {
            Column(modifier = Modifier.background(colors.surface)) {

                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurface
                    ),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.reading_title),
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (items.isNotEmpty() && !(isComplete && pagerState.currentPage == items.size)) {
                                val pageText = BidiHelper.formatPageCounter(
                                    pagerState.currentPage + 1,
                                    items.size,
                                    context
                                )

                                LtrText(
                                    text = pageText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = safeOnBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.reading_back_content_description)
                            )
                        }
                    },
                    actions = {
                        // Vibration toggle button
                        IconButton(onClick = { viewModel.toggleVibration() }) {
                            Icon(
                                imageVector = if (vibrationEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                contentDescription = stringResource(
                                    if (vibrationEnabled) R.string.vibration_enabled_content_description
                                    else R.string.vibration_disabled_content_description
                                ),
                                tint = if (vibrationEnabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        val currentItem = if (isComplete && pagerState.currentPage == items.size) null else items.getOrNull(pagerState.currentPage)
                        if (currentItem != null) {
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                LtrText(
                                    text = if (currentItem.isInfinite) {
                                        "${currentItem.currentRepeats}/∞"
                                    } else {
                                        BidiHelper.formatRepeatCounter(
                                            currentItem.currentRepeats,
                                            currentItem.requiredRepeats,
                                            context
                                        )
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )
                LinearProgressBar(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = colors.primary,
                    trackColor = colors.onSurface.copy(alpha = 0.15f)
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            key = { page -> page }
        ) { page ->
            if (isComplete && page == items.size) {
                CompletionScreen(
                    onBackToSummary = safeOnBack,
                    isEnabled = isActive,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (items.isNotEmpty()) {
                val item = items[page]

                AzkarReadingItem(
                    item = item,
                    onIncrement = {
                        if (!isActive) return@AzkarReadingItem

                        if (item.isInfinite) {
                            if (vibrationEnabled) vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
                            viewModel.incrementRepeat(item.id)
                            return@AzkarReadingItem
                        }

                        val isAlreadyComplete = item.currentRepeats >= item.requiredRepeats

                        if (isAlreadyComplete) {
                            if (page < items.size - 1) {
                                scope.launch {
                                    vibrator.vibrate(VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE))
                                    pagerState.animateScrollToPage(page + 1)
                                }
                            }
                            return@AzkarReadingItem
                        }

                        if (vibrationEnabled) vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
                        viewModel.incrementRepeat(item.id)

                        val willBeComplete = (item.currentRepeats + 1) >= item.requiredRepeats
                        if (willBeComplete) {
                            scope.launch {
                                delay(0)
                                vibrator.vibrate(VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE))
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    },
                    onHoldComplete = {
                        if (!isActive) return@AzkarReadingItem

                        if (holdToComplete) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
                            viewModel.markItemComplete(item.id)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(holdToCompleteDisabledMessage)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LtrText(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = "\u200E$text\u200E",
            style = style.merge(TextStyle(textDirection = TextDirection.Ltr)),
            color = color,
            modifier = modifier
        )
    }
}

@Composable
private fun LinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color
) {
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .drawBehind {
                drawRect(trackColor)
                if (progress > 0f) {
                    val progressWidth = size.width * progress.coerceIn(0f, 1f)
                    val isRtl = layoutDirection == LayoutDirection.Rtl
                    val startX = if (isRtl) size.width - progressWidth else 0f
                    drawRect(
                        color = color,
                        topLeft = Offset(startX, 0f),
                        size = Size(progressWidth, size.height)
                    )
                }
            }
    )
}

