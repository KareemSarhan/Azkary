package com.app.azkary.data.model

data class QuranReference(
    val surahNumber: Int,
    val ayahNumber: Int? = null,
    val ayahNumberEnd: Int? = null
)

data class VerseOfDayUi(
    val surahName: String,
    val ayahText: String,
    val surahNumber: Int,
    val ayahNumber: Int
)

data class QuranSurahUi(
    val surahNumber: Int,
    val surahName: String,
    val bismillah: String?,
    val ayahs: List<AyahUi>
) {
    val totalAyahs: Int get() = ayahs.size
}

data class AyahUi(
    val ayahNumber: Int,
    val text: String
)

sealed class QuranUiState {
    data object Loading : QuranUiState()
    data class Success(val surah: QuranSurahUi) : QuranUiState()
    data object Error : QuranUiState()
}
