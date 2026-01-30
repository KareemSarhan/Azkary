package com.app.azkary.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.azkary.data.local.dao.*
import com.app.azkary.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        CategoryEntity::class,
        CategoryTextEntity::class,
        AzkarItemEntity::class,
        AzkarTextEntity::class,
        CategoryItemCrossRefEntity::class,
        UserProgressEntity::class,
        PrayerMonthEntity::class,
        PrayerDayEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AzkarDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryTextDao(): CategoryTextDao
    abstract fun azkarItemDao(): AzkarItemDao
    abstract fun azkarTextDao(): AzkarTextDao
    abstract fun categoryItemDao(): CategoryItemDao
    abstract fun progressDao(): ProgressDao
    abstract fun prayerMonthDao(): PrayerMonthDao
    abstract fun prayerDayDao(): PrayerDayDao
    suspend fun getDbVersion(): Int {
        return withContext(Dispatchers.IO) {
            val cursor = this@AzkarDatabase.openHelper.readableDatabase
                .query("PRAGMA user_version")
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        }
    }
}
