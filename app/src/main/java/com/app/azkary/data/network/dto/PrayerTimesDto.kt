package com.app.azkary.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrayerCalendarResponse(
    val code: Int,
    val status: String,
    val data: List<PrayerDayDto>
)

@Serializable
data class PrayerDayDto(
    val timings: PrayerTimingsDto,
    val date: DateDto,
    val meta: MetaDto
)

@Serializable
data class PrayerTimingsDto(
    val Fajr: String,
    val Sunrise: String,
    val Dhuhr: String,
    val Asr: String,
    val Sunset: String,
    val Maghrib: String,
    val Isha: String,
    val Firstthird: String,
    val Midnight: String,
    val Lastthird: String
)

@Serializable
data class DateDto(
    val gregorian: GregorianDateDto
)

@Serializable
data class GregorianDateDto(
    val date: String,
    val day: String,
    val month: MonthDto,
    val year: String
)

@Serializable
data class MonthDto(
    val number: Int,
    val en: String
)

@Serializable
data class MetaDto(
    val timezone: String
)
