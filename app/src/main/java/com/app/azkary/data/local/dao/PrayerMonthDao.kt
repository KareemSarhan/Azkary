package com.app.azkary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.azkary.data.local.entities.PrayerMonthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerMonthDao {
    
    @Query("""
        SELECT * FROM prayer_months 
        WHERE year = :year AND month = :month 
        AND latitude = :latitude AND longitude = :longitude 
        AND methodId = :methodId
        LIMIT 1
    """)
    suspend fun getMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int
    ): PrayerMonthEntity?
    
    @Query("""
        SELECT * FROM prayer_months 
        WHERE year = :year AND month = :month 
        AND latitude = :latitude AND longitude = :longitude 
        AND methodId = :methodId
        LIMIT 1
    """)
    fun observeMonth(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int
    ): Flow<PrayerMonthEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonth(month: PrayerMonthEntity): Long
    
    @Update
    suspend fun updateMonth(month: PrayerMonthEntity)
    
    @Query("DELETE FROM prayer_months WHERE lastUpdated < :cutoffDate")
    suspend fun deleteOldMonths(cutoffDate: java.time.Instant)
    
    @Query("SELECT COUNT(*) FROM prayer_months")
    suspend fun getCount(): Int
}