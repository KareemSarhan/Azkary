package com.app.azkary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.app.azkary.data.local.entities.PrayerDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDayDao {
    
    @Query("SELECT * FROM prayer_days WHERE monthId = :monthId ORDER BY date ASC")
    suspend fun getDaysForMonth(monthId: Long): List<PrayerDayEntity>
    
    @Query("SELECT * FROM prayer_days WHERE monthId = :monthId AND date = :date LIMIT 1")
    suspend fun getDay(monthId: Long, date: java.time.LocalDate): PrayerDayEntity?
    
    @Query("SELECT * FROM prayer_days WHERE monthId = :monthId AND date = :date LIMIT 1")
    fun observeDay(monthId: Long, date: java.time.LocalDate): Flow<PrayerDayEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: PrayerDayEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<PrayerDayEntity>)
    
    @Transaction
    suspend fun upsertMonth(monthId: Long, days: List<PrayerDayEntity>) {
        // Delete existing days for this month
        deleteDaysForMonth(monthId)
        // Insert new days
        insertDays(days)
    }
    
    @Query("DELETE FROM prayer_days WHERE monthId = :monthId")
    suspend fun deleteDaysForMonth(monthId: Long)
    
    @Query("SELECT COUNT(*) FROM prayer_days WHERE monthId = :monthId")
    suspend fun getDayCountForMonth(monthId: Long): Int
    
    @Query("SELECT * FROM prayer_days WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDaysInRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): List<PrayerDayEntity>
}