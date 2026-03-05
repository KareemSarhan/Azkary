package com.app.azkary.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidAppUpdateManagerFactory @Inject constructor() : AppUpdateManagerFactory {
    override fun create(activity: ComponentActivity, lifecycleOwner: LifecycleOwner): AppUpdateManager {
        return NoOpAppUpdateManager()
    }
}
