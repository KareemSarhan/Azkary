package com.app.azkary.data.local.entities

import androidx.room.*
import com.app.azkary.data.model.*

@Entity(
    tableName = "categories",
    indices = [Index("sortOrder")]
)
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val type: CategoryType,
    val systemKey: SystemCategoryKey? = null,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val from: Int,
    val to: Int,
    val notificationEnabled: Boolean = false
)

@Entity(
    tableName = "category_texts",
    primaryKeys = ["categoryId", "langTag"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class CategoryTextEntity(
    val categoryId: String,
    val langTag: String,
    val name: String
)

@Entity(
    tableName = "azkar_items"
)
data class AzkarItemEntity(
    @PrimaryKey val itemId: String,
    val requiredRepeats: Int,
    val source: AzkarSource,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isInfinite: Boolean = false
)

@Entity(
    tableName = "azkar_texts",
    primaryKeys = ["itemId", "langTag"],
    foreignKeys = [
        ForeignKey(
            entity = AzkarItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class AzkarTextEntity(
    val itemId: String,
    val langTag: String,
    val title: String? = null,
    val text: String? = null,
    val translation: String? = null,
    val referenceText: String? = null
)

@Entity(
    tableName = "category_item_crossrefs",
    primaryKeys = ["categoryId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AzkarItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("itemId"),
        Index("categoryId", "sortOrder")
    ]
)
data class CategoryItemCrossRefEntity(
    val categoryId: String,
    val itemId: String,
    val sortOrder: Int,
    val isEnabled: Boolean = true,
    val requiredRepeats: Int = 1,
    val isInfinite: Boolean = false
)

@Entity(
    tableName = "user_progress",
    primaryKeys = ["categoryId", "itemId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AzkarItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("itemId"),
        Index("categoryId", "date")
    ]
)
data class UserProgressEntity(
    val categoryId: String,
    val itemId: String,
    val date: String,
    val currentRepeats: Int,
    val isCompleted: Boolean
)
