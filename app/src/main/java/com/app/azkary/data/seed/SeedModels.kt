package com.app.azkary.data.seed

import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.SystemCategoryKey
import kotlinx.serialization.Serializable

@Serializable
data class SeedPack(
    val schemaVersion: Int,
    val generatedAt: String,
    val categories: List<SeedCategory>,
    val items: List<SeedItem>
)

@Serializable
data class SeedCategory(
    val categoryId: String,
    val systemKey: SystemCategoryKey? = null,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val texts: Map<String, SeedCategoryText>,
    val items: List<SeedCategoryItemRef>,
    val from: Int,
    val to: Int
)

@Serializable
data class SeedCategoryText(
    val name: String
)

@Serializable
data class SeedCategoryItemRef(
    val itemId: String
)

@Serializable
data class SeedItem(
    val itemId: String,
    val requiredRepeats: Int,
    val source: AzkarSource = AzkarSource.SEEDED,
    val texts: Map<String, SeedItemText>
)

@Serializable
data class SeedItemText(
    val title: String? = null,
    val text: String? = null, // Used for Arabic or Transliteration
    val translation: String? = null,
    val referenceText: String? = null
)
