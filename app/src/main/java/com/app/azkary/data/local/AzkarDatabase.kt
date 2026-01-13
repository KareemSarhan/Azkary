package com.app.azkary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.azkary.data.local.dao.*
import com.app.azkary.data.local.entities.*

@Database(
    entities = [
        CategoryEntity::class,
        CategoryTextEntity::class,
        AzkarItemEntity::class,
        AzkarTextEntity::class,
        CategoryItemCrossRefEntity::class,
        UserProgressEntity::class
    ],
    version = 2, // Upgraded version for new schema
    exportSchema = false
)
abstract class AzkarDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryTextDao(): CategoryTextDao
    abstract fun azkarItemDao(): AzkarItemDao
    abstract fun azkarTextDao(): AzkarTextDao
    abstract fun categoryItemDao(): CategoryItemDao
    abstract fun progressDao(): ProgressDao
}
