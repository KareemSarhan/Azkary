package com.app.azkary.domain

import android.app.Activity

interface AppRatingManager {
    suspend fun shouldShowRatingPrompt(): Boolean
    fun requestReview(activity: Activity, onComplete: () -> Unit = {})
    fun requestManualReview(activity: Activity, onComplete: () -> Unit = {})
}
