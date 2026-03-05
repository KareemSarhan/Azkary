package com.app.azkary.data.model

data class CategoryUi(
    val id: String,
    val name: String,
    val type: CategoryType,
    val systemKey: SystemCategoryKey?,
    val progress: Float,
    val from: Int,
    val to: Int
)

data class AzkarItemUi(
    val id: String,
    val title: String?,
    val arabicText: String?,
    val transliteration: String?,
    val translation: String?,
    val reference: String?,
    val requiredRepeats: Int,
    val currentRepeats: Int,
    val isCompleted: Boolean,
    val isInfinite: Boolean = false
)

data class AvailableZikr(
    val id: String,
    val title: String?,
    val arabicText: String?,
    val transliteration: String?,
    val translation: String?,
    val requiredRepeats: Int,
    val source: AzkarSource
)

data class CategoryItemConfig(
    val itemId: String,
    val requiredRepeats: Int,
    val isInfinite: Boolean
)

/**
 * Represents progress for a single day in the weekly view
 */
data class DayProgress(
    val date: String, // ISO date string YYYY-MM-DD
    val dayOfWeek: Int, // 1 = Sunday, 7 = Saturday
    val dayOfMonth: Int, // 1-31
    val progress: Float, // 0.0 - 1.0 aggregated across all categories
    val isToday: Boolean
)

/**
 * UI state for the weekly progress card showing last 7 days
 */
data class WeeklyProgressUi(
    val days: List<DayProgress>
)
