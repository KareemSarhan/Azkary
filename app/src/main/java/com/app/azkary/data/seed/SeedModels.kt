package com.app.azkary.data.seed

import com.app.azkary.data.model.AzkarSource
import com.app.azkary.data.model.SystemCategoryKey
import kotlinx.serialization.Serializable

/**
 * Manifest file that lists all seed files to load.
 * This enables splitting large seed data into multiple organized files.
 */
@Serializable
data class SeedManifest(
    val schemaVersion: Int,
    val generatedAt: String,
    val files: List<String>
)

/**
 * Individual seed file containing categories and items for a specific category type.
 */
@Serializable
data class SeedFile(
    val categoryType: String,
    val categories: List<SeedCategory>,
    val items: List<SeedItem>
)

/**
 * Legacy SeedPack for backward compatibility with single-file seeds.
 */
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
