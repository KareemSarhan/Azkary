package com.app.azkary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    override fun toString(): String = "%.4f, %.4f".format(latitude, longitude)

    fun toReadableString(): String = 
        "${if (latitude >= 0) "N" else "S"} ${kotlin.math.abs(latitude).format(4)}, " +
        "${if (longitude >= 0) "E" else "W"} ${kotlin.math.abs(longitude).format(4)}"
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
