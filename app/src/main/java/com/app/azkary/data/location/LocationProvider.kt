package com.app.azkary.data.location

import android.location.Location

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
}
