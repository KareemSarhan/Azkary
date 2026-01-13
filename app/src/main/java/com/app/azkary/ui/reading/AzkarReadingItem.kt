package com.app.azkary.ui.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.azkary.data.model.AzkarItemUi

@Composable
fun AzkarReadingItem(
    item: AzkarItemUi,
    currentCount: Int,
    onIncrement: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { if (currentCount < item.requiredRepeats) onIncrement() },
        color = colors.background // ✅ respects dark/light
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic Text (keeps RTL naturally)
            item.arabicText?.let { arabic ->
                Text(
                    text = arabic,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 38.sp,
                        lineHeight = 58.sp,
                        fontFamily = FontFamily.Default,
                        color = colors.onBackground // ✅ theme aware
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }

            // Transliteration (force LTR)
            if (!item.transliteration.isNullOrBlank()) {
                ReadingSectionLtr(
                    title = "Transliteration",
                    content = item.transliteration,
                    titleColor = colors.primary
                )
            }

            // Translation (force LTR)
            if (!item.translation.isNullOrBlank()) {
                ReadingSectionLtr(
                    title = "Translation",
                    content = item.translation,
                    titleColor = colors.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reference card (theme aware + LTR usually better)
            if (!item.reference.isNullOrBlank()) {
                HadithInformationCardLtr(referenceText = item.reference)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ReadingSectionLtr(
    title: String,
    content: String,
    titleColor: androidx.compose.ui.graphics.Color
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = titleColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Force the CONTENT to render LTR even in RTL app
        LtrText(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.onBackground.copy(alpha = 0.9f),
                lineHeight = 24.sp,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun HadithInformationCardLtr(referenceText: String) {
    val colors = MaterialTheme.colorScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            LtrText(
                text = referenceText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reference",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Force LTR rendering for English/Latin content in an RTL app.
 * (Fixes BiDi issues and keeps transliteration readable.)
 */
@Composable
private fun LtrText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = "\u200E$text\u200E",
            style = style.merge(TextStyle(textDirection = TextDirection.Ltr)),
            modifier = modifier
        )
    }
}
