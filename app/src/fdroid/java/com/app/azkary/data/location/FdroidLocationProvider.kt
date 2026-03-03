package com.app.azkary.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * F-Droid flavor implementation using Android's standard LocationManager.
 * This is the FOSS implementation that doesn't rely on Google Play Services.
 */
@Singleton
class FdroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Try to get last known location first from all providers
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                        bestLocation = location
                    }
                } catch (e: Exception) {
                    // Provider not available, continue
                }
            }

            if (bestLocation != null && System.currentTimeMillis() - bestLocation.time < 5 * 60 * 1000) {
                // Use cached location if it's less than 5 minutes old
                continuation.resume(bestLocation)
                return@suspendCancellableCoroutine
            }

            // Request fresh location from network provider (faster than GPS)
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (!continuation.isCompleted) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (!continuation.isCompleted) {
                        continuation.resume(bestLocation)
                    }
                }
            }

            try {
                // Request location from network provider first (faster)
                locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    locationListener,
                    Looper.getMainLooper()
                )

                // Fallback to GPS if network doesn't respond quickly
                try {
                    locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        locationListener,
                        Looper.getMainLooper()
                    )
                } catch (e: Exception) {
                    // GPS not available
                }
            } catch (e: Exception) {
                // Network provider not available, use best known location
                if (!continuation.isCompleted) {
                    continuation.resume(bestLocation)
                }
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(locationListener)
            }

            // Timeout fallback
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!continuation.isCompleted) {
                    locationManager.removeUpdates(locationListener)
                    continuation.resume(bestLocation)
                }
            }, 10000) // 10 second timeout
        }
}
