package com.app.azkary.data.repository

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface GeocodingRepository {
    suspend fun getCityName(latitude: Double, longitude: Double): String?
}

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : GeocodingRepository {

    @Suppress("DEPRECATION")
    override suspend fun getCityName(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
                val address = results.firstOrNull() ?: return@withContext null

                // Try to get city and country
                val city = address.locality 
                    ?: address.subAdminArea 
                    ?: address.adminArea

                val country = address.countryCode ?: address.countryName

                // Return formatted string
                when {
                    city != null && country != null -> "$city, $country"
                    city != null -> city
                    country != null -> country
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
}
