package com.app.azkary.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryItemCrossRefEntity
import com.app.azkary.data.local.entities.CategoryTextEntity
import com.app.azkary.data.model.AzkarSource
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

    suspend fun seedIfNeeded(context: Context) {
        try {
            val manifestJsonString = context.assets
                .open("seed/manifest.json")
                .bufferedReader()
                .use { it.readText() }

            val manifest = jsonConfig.decodeFromString<SeedManifest>(manifestJsonString)
            val currentDbVersion = database.getDbVersion()

            val categoryCount = database.categoryDao().getActiveCategoriesOrdered().first().size
            val isDbEmpty = categoryCount == 0

            println("DEBUG: SeedManager - Current DB version: $currentDbVersion, Seed schema version: ${manifest.schemaVersion}")
            println("DEBUG: SeedManager - Category count: $categoryCount, Database empty: $isDbEmpty")

            if (currentDbVersion < manifest.schemaVersion || isDbEmpty) {
                println("DEBUG: SeedManager - Starting seed import from split files")
                val categories = loadCategories(context, manifest)
                val items = loadAndMergeItems(context, manifest)
                importSeedData(categories, items)
                println("DEBUG: SeedManager - Seed import completed")
            } else {
                println("DEBUG: SeedManager - Skipping seed import - DB version is up to date and has data")
            }
        } catch (e: Exception) {
            println("DEBUG: SeedManager - Error during seeding: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun loadCategories(context: Context, manifest: SeedManifest): List<SeedCategory> {
        val categoriesJsonString = context.assets
            .open("seed/${manifest.categoriesFile}")
            .bufferedReader()
            .use { it.readText() }

        return jsonConfig.decodeFromString<SeedCategoriesFile>(categoriesJsonString).categories
    }

    private suspend fun loadAndMergeItems(context: Context, manifest: SeedManifest): Map<String, SeedItem> {
        val mergedItems = mutableMapOf<String, MutableSeedItem>()
        val arItems = mutableMapOf<String, SeedItemLocalized>()
        val enItems = mutableMapOf<String, SeedItemLocalized>()

        manifest.itemsFiles.forEach { (langCode, filePath) ->
            val jsonString = context.assets
                .open("seed/$filePath")
                .bufferedReader()
                .use { it.readText() }

            val itemsFile = jsonConfig.decodeFromString<SeedItemsFile>(jsonString)

            require(itemsFile.language == langCode) {
                "Language mismatch: file claims ${itemsFile.language}, expected $langCode"
            }

            itemsFile.items.forEach { item ->
                when (langCode) {
                    "ar" -> arItems[item.itemId] = item
                    "en" -> enItems[item.itemId] = item
                }
            }
        }

        val allItemIds = arItems.keys + enItems.keys

        allItemIds.forEach { itemId ->
            val arItem = arItems[itemId]
            val enItem = enItems[itemId]

            if (arItem == null && enItem != null) {
                println("DEBUG: SeedManager - Warning: Item $itemId exists in EN but not AR, skipping")
                return@forEach
            }

            val baseItem = arItem ?: enItem!!

            val existingItem = mergedItems[itemId]
            if (existingItem != null && existingItem.requiredRepeats != baseItem.requiredRepeats) {
                throw IllegalStateException(
                    "requiredRepeats mismatch for $itemId: AR has ${arItem?.requiredRepeats}, EN has ${enItem?.requiredRepeats}"
                )
            }

            val texts = mutableMapOf<String, SeedItemText>()

            arItem?.let {
                texts["ar"] = SeedItemText(
                    title = it.title,
                    text = it.text,
                    translation = it.translation,
                    referenceText = it.referenceText
                )
            }

            if (enItem != null) {
                texts["en"] = SeedItemText(
                    title = enItem.title,
                    text = enItem.text,
                    translation = enItem.translation,
                    referenceText = enItem.referenceText
                )
            } else if (arItem != null) {
                println("DEBUG: SeedManager - Warning: Item $itemId missing EN translation, using AR as fallback")
                texts["en"] = SeedItemText(
                    title = arItem.title,
                    text = arItem.text,
                    translation = arItem.translation,
                    referenceText = arItem.referenceText
                )
            }

            mergedItems[itemId] = MutableSeedItem(
                itemId = itemId,
                requiredRepeats = baseItem.requiredRepeats,
                source = arItem?.source ?: enItem?.source ?: AzkarSource.SEEDED,
                texts = texts
            )
        }

        return mergedItems.mapValues { (_, mutableItem) ->
            SeedItem(
                itemId = mutableItem.itemId,
                requiredRepeats = mutableItem.requiredRepeats,
                source = mutableItem.source,
                texts = mutableItem.texts
            )
        }
    }

    private data class MutableSeedItem(
        val itemId: String,
        val requiredRepeats: Int,
        val source: AzkarSource,
        val texts: Map<String, SeedItemText>
    )

    private suspend fun importSeedData(categories: List<SeedCategory>, items: Map<String, SeedItem>) {
        database.withTransaction {
            val categoryDao = database.categoryDao()
            val categoryTextDao = database.categoryTextDao()
            val itemDao = database.azkarItemDao()
            val textDao = database.azkarTextDao()
            val crossRefDao = database.categoryItemDao()

            items.values.forEach { seedItem ->
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

            categories.forEach { seedCat ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = seedCat.categoryId,
                        type = if (seedCat.systemKey != null) CategoryType.DEFAULT else CategoryType.USER,
                        systemKey = seedCat.systemKey,
                        sortOrder = seedCat.sortOrder,
                        isArchived = seedCat.isArchived,
                        from = seedCat.from,
                        to = seedCat.to,
                        notificationEnabled = seedCat.systemKey != null
                    )
                )

                val categoryTexts = seedCat.texts.map { (lang, text) ->
                    CategoryTextEntity(seedCat.categoryId, lang, text.name)
                }
                categoryTextDao.upsertCategoryTexts(categoryTexts)

                seedCat.items.forEachIndexed { index, ref ->
                    val seedItem = items[ref.itemId]
                    if (seedItem != null) {
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
}
