package com.app.azkary.data.repository

import android.content.Context
import com.app.azkary.data.local.dao.AzkarItemDao
import com.app.azkary.data.local.dao.AzkarTextDao
import com.app.azkary.data.local.dao.CategoryDao
import com.app.azkary.data.local.dao.CategoryItemDao
import com.app.azkary.data.local.dao.CategoryTextDao
import com.app.azkary.data.local.dao.ProgressDao
import com.app.azkary.data.local.entities.UserProgressEntity
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.seed.SeedManager
import com.app.azkary.util.ArabicNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
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
        fallbacks: List<String>
    ): T? {
        if (texts.isEmpty()) return null
        val textMap = texts.associateBy { langTagSelector(it) }
        
        textMap[preferredLang]?.let { return it }
        val baseLang = preferredLang.substringBefore('-')
        textMap[baseLang]?.let { return it }
        fallbacks.forEach { fallback -> textMap[fallback]?.let { return it } }
        
        return texts.first()
    }

    fun observeCategoriesWithDisplayName(
        langTag: String,
        fallbackTags: List<String>
    ): Flow<List<CategoryUi>> {
        return categoryDao.getActiveCategoriesOrdered().flatMapLatest { categories ->
            if (categories.isEmpty()) return@flatMapLatest flowOf(emptyList())
            
            val flows = categories.map { category ->
                val weightedProgressFlow = getWeightedProgress(
                    category.categoryId, 
                    LocalDate.now().toString(), 
                    langTag
                )
                
                categoryTextDao.getCategoryTexts(category.categoryId).map { texts ->
                    val bestText = selectBestText(texts, { it.langTag }, langTag, fallbackTags)
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
        fallbackTags: List<String>,
        date: String = LocalDate.now().toString()
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
                    val bestText = selectBestText(texts, { it.langTag }, langTag, fallbackTags)
                    val progress = progressMap[crossRef.itemId]
                    
                    AzkarItemUi(
                        id = item.itemId,
                        title = bestText?.title,
                        arabicText = arabicText,
                        transliteration = if (langTag != "ar") bestText?.text else null,
                        translation = bestText?.translation,
                        reference = bestText?.referenceText,
                        requiredRepeats = item.requiredRepeats,
                        currentRepeats = progress?.currentRepeats ?: 0,
                        isCompleted = progress?.isCompleted ?: false
                    )
                }
            }
        }.flatMapLatest { flows ->
            if (flows.isEmpty()) flowOf(emptyList())
            else combine(flows) { it.toList() }
        }
    }

    suspend fun incrementRepeat(categoryId: String, itemId: String, date: String) {
        val item = itemDao.getItemById(itemId).first() ?: return
        val currentProgress = progressDao.observeProgressForItem(categoryId, itemId, date).first()
        val currentCount = currentProgress?.currentRepeats ?: 0
        val newCount = currentCount + 1
        
        progressDao.upsertProgress(
            UserProgressEntity(
                categoryId = categoryId,
                itemId = itemId,
                date = date,
                currentRepeats = newCount,
                isCompleted = newCount >= item.requiredRepeats
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
                    val weightPerOne = ArabicNormalizer.normalize(projection.text.text).length
                    totalWeight += weightPerOne.toLong() * projection.item.requiredRepeats
                    
                    val progress = progressMap[projection.item.itemId]
                    val doneRepeats = progress?.currentRepeats?.coerceAtMost(projection.item.requiredRepeats) ?: 0
                    completedWeight += weightPerOne.toLong() * doneRepeats
                }
                
                if (totalWeight == 0L) 0f else completedWeight.toFloat() / totalWeight
            }
        }
    }

    suspend fun seedDatabase() {
        seedManager.seedIfNeeded(context)
    }
}