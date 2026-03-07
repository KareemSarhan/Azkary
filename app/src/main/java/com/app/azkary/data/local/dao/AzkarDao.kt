package com.app.azkary.data.local.dao

import androidx.room.*
import com.app.azkary.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder")
    fun getActiveCategoriesOrdered(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isArchived = 1 WHERE categoryId = :categoryId AND type = 'USER'")
    suspend fun archiveCategory(categoryId: String)

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE categoryId = :categoryId")
    suspend fun updateSortOrder(categoryId: String, sortOrder: Int)

    @Query("DELETE FROM categories WHERE categoryId = :categoryId AND type = 'USER'")
    suspend fun deleteCategory(categoryId: String)

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    fun getCategoryById(categoryId: String): Flow<CategoryEntity?>

    @Query("SELECT MAX(sortOrder) FROM categories WHERE isArchived = 0")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT * FROM categories WHERE systemKey = :systemKey LIMIT 1")
    suspend fun getCategoryBySystemKey(systemKey: String): CategoryEntity?
}

@Dao
interface CategoryTextDao {
    @Upsert
    suspend fun upsertCategoryTexts(texts: List<CategoryTextEntity>)

    @Query("SELECT * FROM category_texts WHERE categoryId = :categoryId")
    fun getCategoryTexts(categoryId: String): Flow<List<CategoryTextEntity>>

    @Upsert
    suspend fun upsertCategoryText(text: CategoryTextEntity)

    @Query("DELETE FROM category_texts WHERE categoryId = :categoryId")
    suspend fun deleteCategoryTexts(categoryId: String)
}

@Dao
interface AzkarItemDao {
    @Upsert
    suspend fun upsertItems(items: List<AzkarItemEntity>)

    @Query("SELECT * FROM azkar_items WHERE itemId = :itemId")
    fun getItemById(itemId: String): Flow<AzkarItemEntity?>

    @Upsert
    suspend fun upsertItem(item: AzkarItemEntity)

    @Query("SELECT * FROM azkar_items WHERE source IN ('SEEDED', 'USER') ORDER BY itemId")
    fun getAvailableItems(): Flow<List<AzkarItemEntity>>

    @Query("DELETE FROM azkar_items WHERE itemId = :itemId AND source = 'USER'")
    suspend fun deleteCustomItem(itemId: String)
}

@Dao
interface AzkarTextDao {
    @Upsert
    suspend fun upsertTexts(texts: List<AzkarTextEntity>)

    @Query("SELECT * FROM azkar_texts WHERE itemId = :itemId")
    fun getTextsForItem(itemId: String): Flow<List<AzkarTextEntity>>

    @Query("SELECT * FROM azkar_texts WHERE itemId IN (:itemIds) AND langTag = :langTag")
    fun getTextsForItems(itemIds: List<String>, langTag: String): Flow<List<AzkarTextEntity>>

    @Upsert
    suspend fun upsertText(text: AzkarTextEntity)

    @Query("DELETE FROM azkar_texts WHERE itemId = :itemId")
    suspend fun deleteTextsForItem(itemId: String)
}

data class ItemWithTextProjection(
    val item_itemId: String,
    val item_source: com.app.azkary.data.model.AzkarSource,
    val item_createdAt: Long,
    val item_updatedAt: Long,
    val text_itemId: String,
    val text_langTag: String,
    val text_title: String?,
    val text_text: String?,
    val text_translation: String?,
    val text_referenceText: String?,
    val sortOrder: Int,
    val requiredRepeats: Int,
    val isInfinite: Boolean
)

data class ItemWeightProjection(
    val itemId: String,
    val requiredRepeats: Int,
    val isInfinite: Boolean,
    @ColumnInfo(name = "arabicText")
    val arabicText: String?
)

@Dao
interface CategoryItemDao {
    @Transaction
    @Query("""
        SELECT 
            ai.itemId as item_itemId, ai.source as item_source, ai.createdAt as item_createdAt, ai.updatedAt as item_updatedAt,
            at.itemId as text_itemId, at.langTag as text_langTag, at.title as text_title, at.text as text_text, at.translation as text_translation, at.referenceText as text_referenceText,
            cicr.sortOrder,
            cicr.requiredRepeats,
            cicr.isInfinite
        FROM azkar_items ai
        JOIN category_item_crossrefs cicr ON ai.itemId = cicr.itemId
        LEFT JOIN azkar_texts at ON ai.itemId = at.itemId AND at.langTag = :langTag
        WHERE cicr.categoryId = :categoryId AND cicr.isEnabled = 1
        ORDER BY cicr.sortOrder
    """)
    fun getEnabledItemsWithText(categoryId: String, langTag: String): Flow<List<ItemWithTextProjection>>

    @Query("""
        SELECT ai.itemId, cicr.requiredRepeats, cicr.isInfinite, at.text as arabicText FROM azkar_items ai
        JOIN category_item_crossrefs cicr ON ai.itemId = cicr.itemId
        LEFT JOIN azkar_texts at ON ai.itemId = at.itemId AND at.langTag = 'ar'
        WHERE cicr.categoryId = :categoryId AND cicr.isEnabled = 1
    """)
    fun getWeightsForCategory(categoryId: String): Flow<List<ItemWeightProjection>>

    @Query("SELECT * FROM category_item_crossrefs WHERE categoryId = :categoryId ORDER BY sortOrder")
    fun getAllCrossRefsForCategory(categoryId: String): Flow<List<CategoryItemCrossRefEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CategoryItemCrossRefEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<CategoryItemCrossRefEntity>)

    @Update
    suspend fun updateCrossRef(crossRef: CategoryItemCrossRefEntity)

    @Query("DELETE FROM category_item_crossrefs WHERE categoryId = :categoryId")
    suspend fun deleteCategoryItems(categoryId: String)

    @Query("DELETE FROM category_item_crossrefs WHERE categoryId = :categoryId AND itemId = :itemId")
    suspend fun removeItemFromCategory(categoryId: String, itemId: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE categoryId = :categoryId AND date = :date")
    fun observeProgressForCategoryDate(categoryId: String, date: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE categoryId = :categoryId AND itemId = :itemId AND date = :date")
    fun observeProgressForItem(categoryId: String, itemId: String, date: String): Flow<UserProgressEntity?>

    @Upsert
    suspend fun upsertProgress(progress: UserProgressEntity)

    @Query("DELETE FROM user_progress WHERE categoryId = :categoryId")
    suspend fun deleteProgressForCategory(categoryId: String)

    /**
     * Get all progress entries for a date range (inclusive)
     * Used for weekly progress calculation
     */
    @Query("SELECT * FROM user_progress WHERE date >= :startDate AND date <= :endDate")
    fun observeProgressForDateRange(startDate: String, endDate: String): Flow<List<UserProgressEntity>>
}
