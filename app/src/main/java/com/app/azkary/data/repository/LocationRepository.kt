package com.app.azkary.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.app.azkary.data.location.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps the LocationProvider to handle permission checks.
 * The actual location implementation is provided by flavor-specific modules.
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider
) : LocationRepository {

    override suspend fun getCurrentLocation(): Location? {
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarseLocation && !hasFineLocation) {
            return null
        }

        return try {
            locationProvider.getCurrentLocation()
        } catch (e: Exception) {
            null
        }
    }
}

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
}
