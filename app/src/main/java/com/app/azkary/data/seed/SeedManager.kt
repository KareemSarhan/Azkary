package com.app.azkary.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryItemCrossRefEntity
import com.app.azkary.data.local.entities.CategoryTextEntity
import com.app.azkary.data.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedManager @Inject constructor(
    private val database: AzkarDatabase,
    private val json: Json
) {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    companion object {
        // Manifest file that lists all seed files to load
        private const val SEED_MANIFEST_FILE = "seed_manifest.json"
        // Legacy single seed file (for backward compatibility)
        private const val LEGACY_SEED_FILE = "seed_azkar.json"
    }

    suspend fun seedIfNeeded(context: Context) {
        try {
            val currentDbVersion = database.getDbVersion()
            val categoryCount = database.categoryDao().getActiveCategoriesOrdered().first().size
            val isDbEmpty = categoryCount == 0

            println("DEBUG: SeedManager - Current DB version: $currentDbVersion, Database empty: $isDbEmpty")

            // Try to load from manifest first (new multi-file approach)
            val seedFiles = tryLoadManifest(context)

            if (seedFiles.isNotEmpty()) {
                // Multi-file seed approach
                val maxSchemaVersion = seedFiles.maxOf { it.schemaVersion }
                println("DEBUG: SeedManager - Multi-file seed detected. Max schema version: $maxSchemaVersion")

                if (currentDbVersion < maxSchemaVersion || isDbEmpty) {
                    println("DEBUG: SeedManager - Starting multi-file seed import")
                    importMultipleSeedFiles(seedFiles)
                    println("DEBUG: SeedManager - Multi-file seed import completed")
                } else {
                    println("DEBUG: SeedManager - Skipping seed import - DB version is up to date")
                }
            } else {
                // Fall back to legacy single file approach
                println("DEBUG: SeedManager - Falling back to legacy single-file seed")
                seedFromLegacyFile(context, currentDbVersion, isDbEmpty)
            }
        } catch (e: Exception) {
            println("DEBUG: SeedManager - Error during seeding: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Try to load the manifest file. Returns empty list if manifest doesn't exist.
     */
    private fun tryLoadManifest(context: Context): List<SeedFileInfo> {
        return try {
            val manifestJson = context.assets.open(SEED_MANIFEST_FILE).bufferedReader().use { it.readText() }
            val manifest = jsonConfig.decodeFromString<SeedManifest>(manifestJson)

            println("DEBUG: SeedManager - Manifest loaded. Schema version: ${manifest.schemaVersion}, Files: ${manifest.files}")

            manifest.files.mapNotNull { fileName ->
                try {
                    val fileJson = context.assets.open(fileName).bufferedReader().use { it.readText() }
                    val seedFile = jsonConfig.decodeFromString<SeedFile>(fileJson)
                    SeedFileInfo(
                        fileName = fileName,
                        schemaVersion = manifest.schemaVersion,
                        categoryType = seedFile.categoryType,
                        categories = seedFile.categories,
                        items = seedFile.items
                    )
                } catch (e: Exception) {
                    println("DEBUG: SeedManager - Error loading seed file '$fileName': ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("DEBUG: SeedManager - Manifest not found or invalid: ${e.message}")
            emptyList()
        }
    }

    /**
     * Legacy seed approach - single file
     */
    private suspend fun seedFromLegacyFile(context: Context, currentDbVersion: Int, isDbEmpty: Boolean) {
        try {
            val jsonString = context.assets.open(LEGACY_SEED_FILE).bufferedReader().use { it.readText() }
            val seedPack = jsonConfig.decodeFromString<SeedPack>(jsonString)

            println("DEBUG: SeedManager - Legacy seed schema version: ${seedPack.schemaVersion}")

            if (currentDbVersion < seedPack.schemaVersion || isDbEmpty) {
                println("DEBUG: SeedManager - Starting legacy seed import")
                importSeedPack(seedPack)
                println("DEBUG: SeedManager - Legacy seed import completed")
            } else {
                println("DEBUG: SeedManager - Skipping legacy seed import - DB version is up to date")
            }
        } catch (e: Exception) {
            println("DEBUG: SeedManager - Error loading legacy seed file: ${e.message}")
        }
    }

    /**
     * Import multiple seed files within a single transaction
     */
    private suspend fun importMultipleSeedFiles(seedFiles: List<SeedFileInfo>) {
        database.withTransaction {
            val categoryDao = database.categoryDao()
            val categoryTextDao = database.categoryTextDao()
            val itemDao = database.azkarItemDao()
            val textDao = database.azkarTextDao()
            val crossRefDao = database.categoryItemDao()

            // Collect all items and categories from all files
            val allItems = mutableMapOf<String, SeedItem>()
            val allCategories = mutableListOf<SeedCategory>()

            seedFiles.forEach { seedFile ->
                // Add items to map (avoids duplicates)
                seedFile.items.forEach { item ->
                    allItems[item.itemId] = item
                }
                // Add categories
                allCategories.addAll(seedFile.categories)
            }

            println("DEBUG: SeedManager - Importing ${allItems.size} items and ${allCategories.size} categories")

            // 1. Insert all items first
            allItems.values.forEach { seedItem ->
                itemDao.upsertItems(
                    listOf(
                        AzkarItemEntity(
                            itemId = seedItem.itemId,
                            requiredRepeats = seedItem.requiredRepeats,
                            source = seedItem.source
                        )
                    )
                )

                val itemTexts = seedItem.texts.map { (lang, content) ->
                    AzkarTextEntity(
                        itemId = seedItem.itemId,
                        langTag = lang,
                        title = content.title,
                        text = content.text,
                        translation = content.translation,
                        referenceText = content.referenceText
                    )
                }
                textDao.upsertTexts(itemTexts)
            }

            // 2. Insert all categories
            allCategories.forEach { seedCat ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = seedCat.categoryId,
                        type = if (seedCat.systemKey != null) CategoryType.DEFAULT else CategoryType.USER,
                        systemKey = seedCat.systemKey,
                        sortOrder = seedCat.sortOrder,
                        isArchived = seedCat.isArchived,
                        from = seedCat.from,
                        to = seedCat.to
                    )
                )

                val categoryTexts = seedCat.texts.map { (lang, text) ->
                    CategoryTextEntity(seedCat.categoryId, lang, text.name)
                }
                categoryTextDao.upsertCategoryTexts(categoryTexts)

                // 3. Link items to categories via crossrefs
                seedCat.items.forEachIndexed { index, ref ->
                    if (allItems.containsKey(ref.itemId)) {
                        val seedItem = allItems[ref.itemId]!!
                        crossRefDao.insertCrossRef(
                            CategoryItemCrossRefEntity(
                                categoryId = seedCat.categoryId,
                                itemId = ref.itemId,
                                sortOrder = index,
                                isEnabled = true,
                                requiredRepeats = seedItem.requiredRepeats,
                                isInfinite = false
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Legacy single-file import for backward compatibility
     */
    private suspend fun importSeedPack(seedPack: SeedPack) {
        database.withTransaction {
            val categoryDao = database.categoryDao()
            val categoryTextDao = database.categoryTextDao()
            val itemDao = database.azkarItemDao()
            val textDao = database.azkarTextDao()
            val crossRefDao = database.categoryItemDao()

            // 1. Insert all items first
            val availableItemIds = mutableSetOf<String>()
            val itemMap = mutableMapOf<String, SeedItem>()
            seedPack.items.forEach { seedItem ->
                itemDao.upsertItems(
                    listOf(
                        AzkarItemEntity(
                            itemId = seedItem.itemId,
                            requiredRepeats = seedItem.requiredRepeats,
                            source = seedItem.source
                        )
                    )
                )

                val itemTexts = seedItem.texts.map { (lang, content) ->
                    AzkarTextEntity(
                        itemId = seedItem.itemId,
                        langTag = lang,
                        title = content.title,
                        text = content.text,
                        translation = content.translation,
                        referenceText = content.referenceText
                    )
                }
                textDao.upsertTexts(itemTexts)
                itemMap[seedItem.itemId] = seedItem
                availableItemIds.add(seedItem.itemId)
            }

            // 2. Insert all categories
            seedPack.categories.forEach { seedCat ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = seedCat.categoryId,
                        type = if (seedCat.systemKey != null) CategoryType.DEFAULT else CategoryType.USER,
                        systemKey = seedCat.systemKey,
                        sortOrder = seedCat.sortOrder,
                        isArchived = seedCat.isArchived,
                        from = seedCat.from,
                        to = seedCat.to
                    )
                )

                val categoryTexts = seedCat.texts.map { (lang, text) ->
                    CategoryTextEntity(seedCat.categoryId, lang, text.name)
                }
                categoryTextDao.upsertCategoryTexts(categoryTexts)

                // 3. Link items to categories via crossrefs
                seedCat.items.forEachIndexed { index, ref ->
                    if (availableItemIds.contains(ref.itemId)) {
                        val seedItem = itemMap[ref.itemId]!!
                        crossRefDao.insertCrossRef(
                            CategoryItemCrossRefEntity(
                                categoryId = seedCat.categoryId,
                                itemId = ref.itemId,
                                sortOrder = index,
                                isEnabled = true,
                                requiredRepeats = seedItem.requiredRepeats,
                                isInfinite = false
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Internal data class to hold seed file information
     */
    private data class SeedFileInfo(
        val fileName: String,
        val schemaVersion: Int,
        val categoryType: String,
        val categories: List<SeedCategory>,
        val items: List<SeedItem>
    )
}
