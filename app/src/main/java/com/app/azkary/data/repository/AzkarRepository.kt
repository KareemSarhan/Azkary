package com.app.azkary.data.repository

import android.content.Context
import com.app.azkary.data.local.dao.AzkarItemDao
import com.app.azkary.data.local.dao.AzkarTextDao
import com.app.azkary.data.local.dao.CategoryDao
import com.app.azkary.data.local.dao.CategoryItemDao
import com.app.azkary.data.local.dao.CategoryTextDao
import com.app.azkary.data.local.dao.ProgressDao
import com.app.azkary.data.local.entities.AzkarItemEntity
import com.app.azkary.data.local.entities.AzkarTextEntity
import com.app.azkary.data.local.entities.CategoryEntity
import com.app.azkary.data.local.entities.CategoryItemCrossRefEntity
import com.app.azkary.data.local.entities.CategoryTextEntity
import com.app.azkary.data.local.entities.UserProgressEntity
import com.app.azkary.data.model.AvailableZikr
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.model.CategoryItemConfig
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.seed.SeedManager
import com.app.azkary.util.ArabicNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AzkarRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryTextDao: CategoryTextDao,
    private val itemDao: AzkarItemDao,
    private val textDao: AzkarTextDao,
    private val categoryItemDao: CategoryItemDao,
    private val progressDao: ProgressDao,
    private val seedManager: SeedManager,
    @ApplicationContext private val context: Context
) {

    private fun <T> selectBestText(
        texts: List<T>,
        langTagSelector: (T) -> String,
        preferredLang: String,
    ): T? {
        if (texts.isEmpty()) return null
        val textMap = texts.associateBy { langTagSelector(it) }
        
        textMap[preferredLang]?.let { return it }
        val baseLang = preferredLang.substringBefore('-')
        textMap[baseLang]?.let { return it }
        return texts.first()
    }

    fun observeCategoriesWithDisplayName(
        langTag: String,
        date: String
    ): Flow<List<CategoryUi>> {
        return categoryDao.getActiveCategoriesOrdered().flatMapLatest { categories ->
            if (categories.isEmpty()) return@flatMapLatest flowOf(emptyList())
            
            val flows = categories.map { category ->
                val weightedProgressFlow = getWeightedProgress(
                    category.categoryId,
                    date, 
                    langTag
                )
                
                categoryTextDao.getCategoryTexts(category.categoryId).map { texts ->
                    val bestText = selectBestText(texts, { it.langTag }, langTag)
                    category to (bestText?.name ?: "Unknown")
                }.combine(weightedProgressFlow) { (cat, name), progress ->
                    CategoryUi(
                        id = cat.categoryId,
                        name = name,
                        type = cat.type,
                        systemKey = cat.systemKey,
                        progress = progress,
                        from = cat.from,
                        to = cat.to
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }

    fun observeItemsForCategory(
        categoryId: String,
        langTag: String,
        date: String
    ): Flow<List<AzkarItemUi>> {
        return combine(
            categoryItemDao.getAllCrossRefsForCategory(categoryId),
            progressDao.observeProgressForCategoryDate(categoryId, date)
        ) { crossRefs, progressList ->
            val enabledCrossRefs = crossRefs.filter { it.isEnabled }.sortedBy { it.sortOrder }
            val progressMap = progressList.associateBy { it.itemId }
            
            enabledCrossRefs.map { crossRef ->
                combine(
                    textDao.getTextsForItem(crossRef.itemId),
                    itemDao.getItemById(crossRef.itemId).filterNotNull()
                ) { texts, item ->
                    val arabicText = texts.find { it.langTag == "ar" }?.text
                    val bestText = selectBestText(texts, { it.langTag }, langTag)
                    val progress = progressMap[crossRef.itemId]
                    
                    AzkarItemUi(
                        id = item.itemId,
                        title = bestText?.title,
                        arabicText = arabicText,
                        transliteration = if (langTag != "ar") bestText?.text else null,
                        translation = bestText?.translation,
                        reference = bestText?.referenceText,
                        requiredRepeats = crossRef.requiredRepeats,
                        currentRepeats = progress?.currentRepeats ?: 0,
                        isCompleted = progress?.isCompleted ?: false,
                        isInfinite = crossRef.isInfinite
                    )
                }
            }
        }.flatMapLatest { flows ->
            if (flows.isEmpty()) flowOf(emptyList())
            else combine(flows) { it.toList() }
        }
    }

    suspend fun incrementRepeat(categoryId: String, itemId: String, date: String) {
        val crossRef = categoryItemDao.getAllCrossRefsForCategory(categoryId).first().find { it.itemId == itemId } ?: return
        
        if (crossRef.isInfinite) {
            val currentProgress = progressDao.observeProgressForItem(categoryId, itemId, date).first()
            val currentCount = currentProgress?.currentRepeats ?: 0
            val newCount = currentCount + 1
            
            progressDao.upsertProgress(
                UserProgressEntity(
                    categoryId = categoryId,
                    itemId = itemId,
                    date = date,
                    currentRepeats = newCount,
                    isCompleted = false
                )
            )
            return
        }
        
        val currentProgress = progressDao.observeProgressForItem(categoryId, itemId, date).first()
        val currentCount = currentProgress?.currentRepeats ?: 0
        val newCount = currentCount + 1
        
        progressDao.upsertProgress(
            UserProgressEntity(
                categoryId = categoryId,
                itemId = itemId,
                date = date,
                currentRepeats = newCount,
                isCompleted = newCount >= crossRef.requiredRepeats
            )
        )
    }

    fun getWeightedProgress(categoryId: String, date: String, langTag: String): Flow<Float> {
        return categoryItemDao.getEnabledItemsWithText(categoryId, langTag).flatMapLatest { projections ->
            if (projections.isEmpty()) return@flatMapLatest flowOf(0f)
            
            progressDao.observeProgressForCategoryDate(categoryId, date).map { progressList ->
                val progressMap = progressList.associateBy { it.itemId }
                var totalWeight = 0L
                var completedWeight = 0L
                
                projections.forEach { projection ->
                    if (projection.isInfinite) return@forEach
                    val weightPerOne = ArabicNormalizer.normalize(projection.text_text).length
                    totalWeight += weightPerOne.toLong() * projection.requiredRepeats
                    
                    val progress = progressMap[projection.item_itemId]
                    val doneRepeats = progress?.currentRepeats?.coerceAtMost(projection.requiredRepeats) ?: 0
                    completedWeight += weightPerOne.toLong() * doneRepeats
                }
                
                if (totalWeight == 0L) 0f else completedWeight.toFloat() / totalWeight
            }
        }
    }

    suspend fun seedDatabase() {
        seedManager.seedIfNeeded(context)
    }

    /**
     * Mark all items in a category as complete for a given date
     */
    suspend fun markCategoryComplete(
        categoryId: String,
        date: String
    ) {
        val crossRefs = categoryItemDao.getAllCrossRefsForCategory(categoryId).first()
        crossRefs.filter { it.isEnabled }.forEach { crossRef ->
            if (crossRef.isInfinite) return@forEach
            progressDao.upsertProgress(
                UserProgressEntity(
                    categoryId = categoryId,
                    itemId = crossRef.itemId,
                    date = date,
                    currentRepeats = crossRef.requiredRepeats,
                    isCompleted = true
                )
            )
        }
    }

    /**
     * Mark all items in a category as incomplete for a given date
     */
    suspend fun markCategoryIncomplete(
        categoryId: String,
        date: String
    ) {
        val crossRefs = categoryItemDao.getAllCrossRefsForCategory(categoryId).first()
        crossRefs.filter { it.isEnabled }.forEach { crossRef ->
            progressDao.upsertProgress(
                UserProgressEntity(
                    categoryId = categoryId,
                    itemId = crossRef.itemId,
                    date = date,
                    currentRepeats = 0,
                    isCompleted = false
                )
            )
        }
    }

    /**
     * Mark a single item as complete
     */
    suspend fun markItemComplete(
        categoryId: String,
        itemId: String,
        date: String
    ) {
        val crossRef = categoryItemDao.getAllCrossRefsForCategory(categoryId).first().find { it.itemId == itemId } ?: return
        progressDao.upsertProgress(
            UserProgressEntity(
                categoryId = categoryId,
                itemId = itemId,
                date = date,
                currentRepeats = crossRef.requiredRepeats,
                isCompleted = true
            )
        )
    }

    suspend fun createCustomCategory(
        name: String,
        langTag: String,
        itemConfigs: List<CategoryItemConfig>,
        from: Int = 0,
        to: Int = 6
    ): String {
        val categoryId = "user_${UUID.randomUUID()}"
        
        val maxSortOrder = categoryDao.getMaxSortOrder() ?: -1
        
        categoryDao.insertCategory(CategoryEntity(
            categoryId = categoryId,
            type = com.app.azkary.data.model.CategoryType.USER,
            systemKey = null,
            sortOrder = maxSortOrder + 1,
            isArchived = false,
            from = from,
            to = to
        ))
        
        categoryTextDao.upsertCategoryText(CategoryTextEntity(
            categoryId = categoryId,
            langTag = langTag,
            name = name
        ))
        
        itemConfigs.forEachIndexed { index, config ->
            categoryItemDao.insertCrossRef(CategoryItemCrossRefEntity(
                categoryId = categoryId,
                itemId = config.itemId,
                sortOrder = index,
                isEnabled = true,
                requiredRepeats = if (config.isInfinite) 0 else config.requiredRepeats,
                isInfinite = config.isInfinite
            ))
            
            // Ensure the item exists
            val existingItem = itemDao.getItemById(config.itemId).first()
            if (existingItem == null) {
                itemDao.upsertItem(AzkarItemEntity(
                    itemId = config.itemId,
                    requiredRepeats = if (config.isInfinite) 0 else config.requiredRepeats,
                    source = com.app.azkary.data.model.AzkarSource.SEEDED,
                    isInfinite = config.isInfinite
                ))
            }
        }
        
        return categoryId
    }

    suspend fun updateCustomCategory(
        categoryId: String,
        name: String? = null,
        itemConfigs: List<CategoryItemConfig>? = null,
        from: Int? = null,
        to: Int? = null
    ) {
        // Check if this is a stock category
        val category = categoryDao.getCategoryById(categoryId).first()
        val isStockCategory = category?.type == com.app.azkary.data.model.CategoryType.DEFAULT
        
        if (name != null && !isStockCategory) {
            // Only allow renaming custom categories
            categoryTextDao.upsertCategoryText(CategoryTextEntity(
                categoryId = categoryId,
                langTag = "en",
                name = name
            ))
            categoryTextDao.upsertCategoryText(CategoryTextEntity(
                categoryId = categoryId,
                langTag = "ar",
                name = name
            ))
        }
        
        // Update schedule for user categories only
        if ((from != null || to != null) && !isStockCategory && category != null) {
            categoryDao.updateCategory(category.copy(
                from = from ?: category.from,
                to = to ?: category.to
            ))
        }
        
        if (itemConfigs != null) {
            if (isStockCategory) {
                // For stock categories: only update item counts, don't add/remove items
                val existingCrossRefs = categoryItemDao.getAllCrossRefsForCategory(categoryId).first()
                val crossRefMap = existingCrossRefs.associateBy { it.itemId }
                
                for (config in itemConfigs) {
                    val existingCrossRef = crossRefMap[config.itemId]
                    if (existingCrossRef != null) {
                        categoryItemDao.updateCrossRef(existingCrossRef.copy(
                            requiredRepeats = if (config.isInfinite) 0 else config.requiredRepeats,
                            isInfinite = config.isInfinite
                        ))
                    }
                }
            } else {
                // For user categories: full update including items
                categoryItemDao.deleteCategoryItems(categoryId)
                itemConfigs.forEachIndexed { index, config ->
                    categoryItemDao.insertCrossRef(CategoryItemCrossRefEntity(
                        categoryId = categoryId,
                        itemId = config.itemId,
                        sortOrder = index,
                        isEnabled = true,
                        requiredRepeats = if (config.isInfinite) 0 else config.requiredRepeats,
                        isInfinite = config.isInfinite
                    ))
                }
            }
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategory(categoryId)
    }

    suspend fun createCustomZikr(
        arabicText: String,
        transliteration: String,
        translation: String,
        reference: String,
        requiredRepeats: Int,
        isInfinite: Boolean,
        langTag: String
    ): String {
        val itemId = "user_${UUID.randomUUID()}"
        
        itemDao.upsertItem(AzkarItemEntity(
            itemId = itemId,
            requiredRepeats = if (isInfinite) 0 else requiredRepeats,
            source = com.app.azkary.data.model.AzkarSource.USER,
            isInfinite = isInfinite
        ))
        
        // Generate title from available text
        val title = arabicText.takeIf { it.isNotBlank() } 
            ?: transliteration.takeIf { it.isNotBlank() } 
            ?: "Custom Zikr"
        
        // Save Arabic text
        textDao.upsertText(AzkarTextEntity(
            itemId = itemId,
            langTag = "ar",
            title = title,
            text = arabicText.ifBlank { null },
            translation = translation.ifBlank { null },
            referenceText = reference.ifBlank { null }
        ))
        
        // Save transliteration for current language
        textDao.upsertText(AzkarTextEntity(
            itemId = itemId,
            langTag = langTag,
            title = title,
            text = transliteration.ifBlank { null },
            translation = translation.ifBlank { null },
            referenceText = reference.ifBlank { null }
        ))
        
        // Also save to English as fallback
        if (langTag != "en") {
            textDao.upsertText(AzkarTextEntity(
                itemId = itemId,
                langTag = "en",
                title = title,
                text = transliteration.ifBlank { null },
                translation = translation.ifBlank { null },
                referenceText = reference.ifBlank { null }
            ))
        }
        
        return itemId
    }


    suspend fun reorderCategories(categoryIds: List<String>) {
        categoryIds.forEachIndexed { index, categoryId ->
            categoryDao.updateSortOrder(categoryId, index)
        }
    }

    fun observeAvailableItems(langTag: String): Flow<List<AvailableZikr>> {
        return itemDao.getAvailableItems().flatMapLatest { items ->
            if (items.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val flows = items.map { item ->
                textDao.getTextsForItem(item.itemId).map { texts ->
                    val arabicText = texts.find { it.langTag == "ar" }?.text
                    val bestText = selectBestText(texts, { it.langTag }, langTag)

                    AvailableZikr(
                        id = item.itemId,
                        title = bestText?.title,
                        arabicText = arabicText,
                        transliteration = if (langTag != "ar") bestText?.text else null,
                        requiredRepeats = item.requiredRepeats,
                        source = item.source
                    )
                }
            }
            combine(flows) { it.toList() }
        }
    }

    /**
     * Get aggregated progress for the last 7 days across all categories.
     * Returns a map of date string to progress percentage (0.0 - 1.0).
     */
    fun getWeeklyProgress(langTag: String): Flow<Map<String, Float>> {
        return categoryDao.getActiveCategoriesOrdered().flatMapLatest { categories ->
            if (categories.isEmpty()) return@flatMapLatest flowOf(emptyMap())

            // Calculate date range for last 7 days
            val today = java.time.LocalDate.now()
            val dates = (0..6).map { daysAgo ->
                today.minusDays(daysAgo.toLong()).toString()
            }.reversed() // Oldest to newest

            // Create flows for each date's aggregated progress
            val dateProgressFlows = dates.map { date ->
                val categoryFlows = categories.map { category ->
                    getWeightedProgress(category.categoryId, date, langTag)
                }

                combine(categoryFlows) { progresses ->
                    // Average progress across all categories for this date
                    val avgProgress = if (progresses.isNotEmpty()) {
                        progresses.average().toFloat()
                    } else 0f
                    date to avgProgress
                }
            }

            combine(dateProgressFlows) { dateProgressPairs ->
                dateProgressPairs.toMap()
            }
        }
    }
}
