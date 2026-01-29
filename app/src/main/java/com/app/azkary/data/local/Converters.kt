package com.app.azkary.data.local

import androidx.room.TypeConverter
import com.app.azkary.data.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromCategoryType(value: CategoryType) = value.name

    @TypeConverter
    fun toCategoryType(value: String) = CategoryType.valueOf(value)

    @TypeConverter
    fun fromAzkarSource(value: AzkarSource) = value.name

    @TypeConverter
    fun toAzkarSource(value: String) = AzkarSource.valueOf(value)

    @TypeConverter
    fun fromSystemCategoryKey(value: SystemCategoryKey?) = value?.name

    @TypeConverter
    fun toSystemCategoryKey(value: String?) = value?.let { SystemCategoryKey.valueOf(it) }

    // Prayer times converters
    @TypeConverter
    fun fromInstant(value: Instant) = value.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long) = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate) = value.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long) = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun fromLocalTime(value: LocalTime) = value.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(value: Int) = LocalTime.ofSecondOfDay(value.toLong())
}
