package com.app.azkary.data.local

import androidx.room.TypeConverter
import com.app.azkary.data.model.*

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
}
