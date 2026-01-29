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
    val isCompleted: Boolean
)