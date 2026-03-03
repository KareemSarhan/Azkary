package com.app.azkary.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.app.azkary.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppUpdateManager(
    private val activity: ComponentActivity,
    private val lifecycleOwner: LifecycleOwner
) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var listener: InstallStateUpdatedListener? = null

    fun checkForUpdate() {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startFlexibleUpdate(appUpdateInfo)
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        startImmediateUpdate(appUpdateInfo)
                    }
                }
            }
        }
    }

    private fun startFlexibleUpdate(appUpdateInfo: AppUpdateInfo) {
        listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadySnackbar()
            }
        }
        
        appUpdateManager.registerListener(listener!!)

        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            activity,
            UPDATE_REQUEST_CODE
        )
    }

    private fun startImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.IMMEDIATE,
            activity,
            UPDATE_REQUEST_CODE
        )
    }

    private fun showUpdateReadySnackbar() {
        CoroutineScope(Dispatchers.Main).launch {
            val rootView = activity.window.decorView.rootView
            Snackbar.make(
                rootView,
                activity.getString(R.string.update_ready_message),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(activity.getString(R.string.update_restart)) {
                appUpdateManager.completeUpdate()
            }.show()
        }
    }

    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadySnackbar()
            }
        }
    }

    fun onDestroy() {
        listener?.let { appUpdateManager.unregisterListener(it) }
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}
