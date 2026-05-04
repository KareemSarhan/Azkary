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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.azkary.R
import com.app.azkary.data.model.AzkarItemUi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AzkarReadingItem(
    item: AzkarItemUi,
    onIncrement: () -> Unit,
    onHoldComplete: (() -> Unit)? = null,
    onNavigateToQuran: ((Int) -> Unit)? = null
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
            // Arabic Text - Quran SDK ayahs or database text
            if (item.quranSurah != null) {
                val isFullSurah = item.quranReference?.ayahNumber == null

                if (isFullSurah) {
                    // Surah name header
                    Text(
                        text = item.quranSurah.surahName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    val context = LocalContext.current
                    Text(
                        text = context.resources.getQuantityString(
                            R.plurals.quran_ayah_count,
                            item.quranSurah.totalAyahs,
                            item.quranSurah.totalAyahs
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // Bismillah - centered on its own line
                    item.quranSurah.bismillah?.let { bismillah ->
                        Text(
                            text = bismillah,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 42.sp,
                                fontFamily = FontFamily.Default,
                                color = colors.onBackground,
                                textDirection = TextDirection.Rtl
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Full surah: continuous flowing text with inline ayah markers
                    // Skip the first ayah if it's the bismillah (already shown above)
                    val ayahStartIndex = if (item.quranSurah.bismillah != null && item.quranSurah.ayahs.isNotEmpty()) 1 else 0
                    val surahText = item.quranSurah.ayahs.drop(ayahStartIndex).joinToString(" ") { ayah ->
                        "${ayah.text} \uFD3F${ayah.ayahNumber}\uFD3E"
                    }
                    Text(
                        text = surahText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            lineHeight = 42.sp,
                            fontFamily = FontFamily.Default,
                            color = colors.onBackground,
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp)
                    )
                } else {
                    // Specific ayah(s): filter to the requested range
                    val ref = item.quranReference!!
                    val ayahs = item.quranSurah.ayahs.filter { ayah ->
                        val start = ref.ayahNumber!!
                        val end = ref.ayahNumberEnd ?: start
                        ayah.ayahNumber in start..end
                    }
                    val ayahText = ayahs.joinToString(" ") { ayah ->
                        "${ayah.text} \uFD3F${ayah.ayahNumber}\uFD3E"
                    }
                    Text(
                        text = ayahText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            lineHeight = 42.sp,
                            fontFamily = FontFamily.Default,
                            color = colors.onBackground,
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                item.arabicText?.let { arabic ->
                    Text(
                        text = arabic,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
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

            // Read Surah chip - only show for single-ayah items (full surahs are already shown inline)
            item.quranReference?.let { quranRef ->
                if (quranRef.ayahNumber != null && onNavigateToQuran != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AssistChip(
                        onClick = { onNavigateToQuran(quranRef.surahNumber) },
                        label = {
                            Text(text = stringResource(R.string.quran_read_surah))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    )
                }
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
            text = content,
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

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LtrText(
                text = referenceText,
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
