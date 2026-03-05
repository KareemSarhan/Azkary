package com.app.azkary.data.seed

import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.SystemCategoryKey
import kotlinx.serialization.Serializable

@Serializable
data class SeedManifest(
    val schemaVersion: Int,
    val generatedAt: String,
    val categoriesFile: String = "categories.json",
    val itemsFiles: Map<String, String>
)

@Serializable
data class SeedCategoriesFile(
    val categories: List<SeedCategory>
)

@Serializable
data class SeedItemsFile(
    val language: String,
    val items: List<SeedItemLocalized>
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
data class SeedItemLocalized(
    val itemId: String,
    val requiredRepeats: Int,
    val source: AzkarSource = AzkarSource.SEEDED,
    val title: String? = null,
    val text: String? = null,
    val translation: String? = null,
    val referenceText: String? = null
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
    val text: String? = null,
    val translation: String? = null,
    val referenceText: String? = null
)
