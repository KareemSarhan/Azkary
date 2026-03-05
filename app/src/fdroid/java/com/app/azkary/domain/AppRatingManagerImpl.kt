package com.app.azkary.domain

import android.app.Activity
import android.content.Context
import com.app.azkary.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAppRatingManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : AppRatingManager {
    
    override suspend fun shouldShowRatingPrompt(): Boolean = false
    
    override fun requestReview(activity: Activity, onComplete: () -> Unit) {
        onComplete()
    }
    
    override fun requestManualReview(activity: Activity, onComplete: () -> Unit) {
        onComplete()
    }
}
