package com.app.azkary.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.app.azkary.data.local.AzkarDatabase
import com.app.azkary.data.local.entities.*
import com.app.azkary.data.model.*
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
            if (database.getDbVersion() < seedPack.schemaVersion)
                importSeedPack(seedPack)
        } catch (e: Exception) {
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
                availableItemIds.add(seedItem.itemId)
            }

            // 2. Insert all categories
            seedPack.categories.forEach { seedCat ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = seedCat.categoryId,
                        type = seedCat.type,
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
                seedCat.items.forEach { ref ->
                    if (availableItemIds.contains(ref.itemId)) {
                        crossRefDao.insertCrossRef(
                            CategoryItemCrossRefEntity(
                                categoryId = seedCat.categoryId,
                                itemId = ref.itemId,
                                sortOrder = ref.sortOrder,
                                isEnabled = ref.isEnabled
                            )
                        )
                    }
                }
            }
        }
    }
}
