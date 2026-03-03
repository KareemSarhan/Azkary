package com.app.azkary.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.azkary.R
import com.app.azkary.data.model.DayProgress

@Composable
fun WeeklyProgressCard(
    days: List<DayProgress>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.weekly_progress_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { dayProgress ->
                    DayProgressItem(dayProgress = dayProgress)
                }
            }
        }
    }
}

@Composable
private fun DayProgressItem(
    dayProgress: DayProgress,
    modifier: Modifier = Modifier
) {
    val dayLabel = if (dayProgress.isToday) {
        stringResource(R.string.weekly_progress_today)
    } else {
        getDayLabel(dayProgress.dayOfWeek)
    }
    val progress = dayProgress.progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Circle with day number inside
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            CircularProgressIndicator(
                progress = { 1f },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                strokeWidth = 3.dp,
                modifier = Modifier.matchParentSize()
            )

            // Progress arc
            if (progress > 0) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = if (progress >= 1f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    },
                    strokeWidth = 3.dp,
                    modifier = Modifier.matchParentSize()
                )
            }

            // Day number centered in circle
            Text(
                text = dayProgress.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (dayProgress.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (dayProgress.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day name (or "Today")
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (dayProgress.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            fontWeight = if (dayProgress.isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun getDayLabel(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> stringResource(R.string.weekly_progress_day_sunday)
        2 -> stringResource(R.string.weekly_progress_day_monday)
        3 -> stringResource(R.string.weekly_progress_day_tuesday)
        4 -> stringResource(R.string.weekly_progress_day_wednesday)
        5 -> stringResource(R.string.weekly_progress_day_thursday)
        6 -> stringResource(R.string.weekly_progress_day_friday)
        7 -> stringResource(R.string.weekly_progress_day_saturday)
        else -> ""
    }
}
