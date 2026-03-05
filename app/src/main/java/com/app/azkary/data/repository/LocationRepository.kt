package com.app.azkary.data.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
}
