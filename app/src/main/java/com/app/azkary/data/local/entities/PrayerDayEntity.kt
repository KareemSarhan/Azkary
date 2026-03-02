package com.app.azkary.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalTime

/**
 * Entity for storing daily prayer times
 */
@Entity(
    tableName = "prayer_days",
    foreignKeys = [
        ForeignKey(
            entity = PrayerMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["monthId", "date"], unique = true)
    ]
)
data class PrayerDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthId: Long,
    val date: LocalDate,
    val fajr: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val firstthird: LocalTime,
    val midnight: LocalTime,
    val lastthird: LocalTime
)