package com.app.azkary.domain

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfHostedAppRatingManager @Inject constructor() : AppRatingManager {

    override suspend fun shouldShowRatingPrompt(): Boolean = false

    override fun requestReview(activity: Activity, onComplete: () -> Unit) {
        onComplete()
    }

    override fun requestManualReview(activity: Activity, onComplete: () -> Unit) {
        onComplete()
    }
}
