package com.app.azkary.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayAppRatingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : AppRatingManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override suspend fun shouldShowRatingPrompt(): Boolean {
        return try {
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            preferencesRepository.shouldShowRatingPrompt(versionName)
        } catch (e: Exception) {
            false
        }
    }

    override fun requestReview(activity: Activity, onComplete: () -> Unit) {
        val reviewManager = ReviewManagerFactory.create(activity)
        val request = reviewManager.requestReviewFlow()
        
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { 
                        markAsRated()
                        onComplete() 
                    }
            } else {
                openPlayStore()
                markAsRated()
                onComplete()
            }
        }
    }

    override fun requestManualReview(activity: Activity, onComplete: () -> Unit) {
        requestReview(activity, onComplete)
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun markAsRated() {
        try {
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            scope.launch {
                preferencesRepository.setLastPromptVersion(versionName)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
