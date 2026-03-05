package com.app.azkary.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner

interface AppUpdateManagerFactory {
    fun create(activity: ComponentActivity, lifecycleOwner: LifecycleOwner): AppUpdateManager
}
