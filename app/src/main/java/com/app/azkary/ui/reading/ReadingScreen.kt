package com.app.azkary.ui.reading

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    onBack: () -> Unit,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState(initial = emptyList())
    val weightedProgress by viewModel.weightedProgress.collectAsState(initial = 0f)

    val animatedProgress by animateFloatAsState(
        targetValue = weightedProgress.coerceIn(0f, 1f),
        label = "weightedProgressAnimation"
    )

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize Vibrator
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Helper to trigger vibration
    val performVibration: (Long) -> Unit = { duration ->
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
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
                                text = "Reading",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (items.isNotEmpty()) {
                                val pageText = "${pagerState.currentPage + 1} of ${items.size}"

                                LtrText(
                                    text = pageText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        val currentItem = items.getOrNull(pagerState.currentPage)
                        if (currentItem != null) {
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                LtrText(
                                    text = "${currentItem.currentRepeats} / ${currentItem.requiredRepeats}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = colors.primary,
                    trackColor = colors.onSurface.copy(alpha = 0.15f)
                )
            }
        }
    ) { padding ->
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                val item = items[page]

                AzkarReadingItem(
                    item = item,
                    currentCount = item.currentRepeats,
                    onIncrement = {
                        // 1. Immediate Short Vibration (Feedback for tap)
                        performVibration(50L)
                        
                        // 2. Update Progress in DB
                        viewModel.incrementRepeat(item.id)
                        
                        // 3. Auto-Next Check
                        val willBeComplete = (item.currentRepeats + 1) >= item.requiredRepeats
                        if (willBeComplete && page < items.size - 1) {
                            scope.launch {
                                // Delay so user sees the final count (e.g. 33/33)
                                delay(0)
                                
                                // 4. Long Vibration (Feedback for Zikr completion/change)
                                performVibration(350L)
                                
                                // 5. Smooth Scroll to next Zikr
                                pagerState.animateScrollToPage(page + 1)
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
