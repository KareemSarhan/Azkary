package com.app.azkary.domain

import com.app.azkary.data.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaCalculator {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    fun bearingToKaaba(location: LatLng): Double {
        val latitude = Math.toRadians(location.latitude)
        val kaabaLatitude = Math.toRadians(KAABA_LATITUDE)
        val longitudeDelta = Math.toRadians(KAABA_LONGITUDE - location.longitude)

        val y = sin(longitudeDelta) * cos(kaabaLatitude)
        val x = cos(latitude) * sin(kaabaLatitude) -
            sin(latitude) * cos(kaabaLatitude) * cos(longitudeDelta)

        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    fun relativeDirectionDegrees(bearingDegrees: Double, headingDegrees: Double): Double {
        return normalizeDegrees(bearingDegrees - headingDegrees)
    }

    fun smallestTurnDegrees(bearingDegrees: Double, headingDegrees: Double): Double {
        val delta = relativeDirectionDegrees(bearingDegrees, headingDegrees)
        return if (delta > 180.0) delta - 360.0 else delta
    }

    fun normalizeDegrees(degrees: Double): Double {
        return ((degrees % 360.0) + 360.0) % 360.0
    }
}
