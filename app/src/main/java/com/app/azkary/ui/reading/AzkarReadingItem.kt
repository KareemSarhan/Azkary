package com.app.azkary.ui.reading

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.azkary.R
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.util.BidiHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AzkarReadingItem(
    item: AzkarItemUi,
    onIncrement: () -> Unit,
    onHoldComplete: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .combinedClickable(
                onClick = onIncrement,
                onLongClick = onHoldComplete
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arabic Text
            item.arabicText?.let { arabic ->
                Text(
                    text = arabic,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp, // Reduced further for better fit
                        lineHeight = 42.sp,
                        fontFamily = FontFamily.Default,
                        color = colors.onBackground
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp)
                )
            }

            // Transliteration
            if (!item.transliteration.isNullOrBlank()) {
                ReadingSectionLtr(
                    title = stringResource(R.string.reading_transliteration),
                    content = item.transliteration,
                    titleColor = colors.primary
                )
            }

            // Translation
            if (!item.translation.isNullOrBlank()) {
                ReadingSectionLtr(
                    title = stringResource(R.string.reading_translation),
                    content = item.translation,
                    titleColor = colors.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reference card
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
    val context = LocalContext.current

    // Format content with proper bidirectional handling for mixed text
    val formattedContent = remember(content, context) {
        BidiHelper.formatMixedText(content, context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = titleColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))

        LtrText(
            text = formattedContent,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.onBackground.copy(alpha = 0.9f),
                lineHeight = 22.sp,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
private fun HadithInformationCardLtr(referenceText: String) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Format reference text with proper bidirectional handling
    // This handles mixed Arabic/English references correctly
    val formattedReference = remember(referenceText, context) {
        BidiHelper.formatVerseReference(referenceText, context)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LtrText(
                text = formattedReference,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 18.sp,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.reading_reference),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

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
