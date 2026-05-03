package com.app.azkary.data.model

data class QuranReference(
    val surahNumber: Int,
    val ayahNumber: Int? = null
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
    val ayahs: List<AyahUi>,
    val totalAyahs: Int
)

data class AyahUi(
    val ayahNumber: Int,
    val text: String
)
