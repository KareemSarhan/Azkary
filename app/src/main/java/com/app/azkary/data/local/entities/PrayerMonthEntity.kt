package com.app.azkary.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

/**
 * Entity for storing cached prayer times for a month
 */
@Entity(
    tableName = "prayer_months",
    indices = [
        Index(value = ["year", "month", "latitude", "longitude", "methodId"], unique = true)
    ]
)
data class PrayerMonthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val latitude: Double,
    val longitude: Double,
    val methodId: Int,
    val timezone: String,
    val lastUpdated: Instant = Instant.now()
)