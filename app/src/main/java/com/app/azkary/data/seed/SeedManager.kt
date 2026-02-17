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

    suspend fun seedIfNeeded(context: Context) {
        try {
            val jsonString =
                context.assets.open("seed_azkar.json").bufferedReader().use { it.readText() }

            val seedPack = jsonConfig.decodeFromString<SeedPack>(jsonString)
            val currentDbVersion = database.getDbVersion()
            
            // Check if database is empty
            val categoryCount = database.categoryDao().getActiveCategoriesOrdered().first().size
            val isDbEmpty = categoryCount == 0
            
            println("DEBUG: SeedManager - Current DB version: $currentDbVersion, Seed schema version: ${seedPack.schemaVersion}")
            println("DEBUG: SeedManager - Category count: $categoryCount, Database empty: $isDbEmpty")
             
            if (currentDbVersion < seedPack.schemaVersion || isDbEmpty) {
                println("DEBUG: SeedManager - Starting seed import")
                importSeedPack(seedPack)
                println("DEBUG: SeedManager - Seed import completed")
            } else {
                println("DEBUG: SeedManager - Skipping seed import - DB version is up to date and has data")
            }
        } catch (e: Exception) {
            println("DEBUG: SeedManager - Error during seeding: ${e.message}")
            e.printStackTrace()
        }
    }


    private suspend fun importSeedPack(seedPack: SeedPack) {
        database.withTransaction {
            val categoryDao = database.categoryDao()
            val categoryTextDao = database.categoryTextDao()
            val itemDao = database.azkarItemDao()
            val textDao = database.azkarTextDao()
            val crossRefDao = database.categoryItemDao()

            // 1. Insert all items first
            val availableItemIds = mutableSetOf<String>()
            val itemMap = mutableMapOf<String, SeedItem>()  // Map itemId -> SeedItem
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
                // ONLY if the item was defined in the 'items' array to avoid FK constraint failure
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
}
