package com.app.azkary.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import com.app.azkary.data.update.SelfHostedUpdateInstaller
import com.app.azkary.data.update.SelfHostedUpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfHostedAppUpdateManagerFactory @Inject constructor(
    private val repository: SelfHostedUpdateRepository,
    private val installer: SelfHostedUpdateInstaller
) : AppUpdateManagerFactory {
    override fun create(activity: ComponentActivity, lifecycleOwner: LifecycleOwner): AppUpdateManager {
        return SelfHostedAppUpdateManager(
            activity = activity,
            lifecycleOwner = lifecycleOwner,
            repository = repository,
            installer = installer
        )
    }
}
