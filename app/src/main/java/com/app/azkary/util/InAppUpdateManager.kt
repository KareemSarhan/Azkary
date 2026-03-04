package com.app.azkary.util

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.app.azkary.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

data class UpdateInfo(
    val updateAvailability: Int,
    val isFlexibleAllowed: Boolean,
    val isImmediateAllowed: Boolean,
    val installStatus: Int
) {
    companion object {
        fun fromAppUpdateInfo(info: AppUpdateInfo): UpdateInfo = UpdateInfo(
            updateAvailability = info.updateAvailability(),
            isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            installStatus = info.installStatus()
        )
    }
}

interface PlayUpdateManagerWrapper {
    fun getAppUpdateInfo(onSuccess: (UpdateInfo) -> Unit, onFailure: (Exception) -> Unit)
    fun registerListener(listener: InstallStateUpdatedListener)
    fun unregisterListener(listener: InstallStateUpdatedListener)
    fun startUpdateFlowForResult(requestCode: Int, updateType: Int): Boolean
    fun completeUpdate()
}

class PlayUpdateManagerWrapperImpl(private val activity: ComponentActivity) : PlayUpdateManagerWrapper {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var cachedAppUpdateInfo: AppUpdateInfo? = null

    override fun getAppUpdateInfo(onSuccess: (UpdateInfo) -> Unit, onFailure: (Exception) -> Unit) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                cachedAppUpdateInfo = info
                onSuccess(UpdateInfo.fromAppUpdateInfo(info))
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun registerListener(listener: InstallStateUpdatedListener) {
        appUpdateManager.registerListener(listener)
    }

    override fun unregisterListener(listener: InstallStateUpdatedListener) {
        appUpdateManager.unregisterListener(listener)
    }

    override fun startUpdateFlowForResult(requestCode: Int, updateType: Int): Boolean {
        val info = cachedAppUpdateInfo ?: return false
        return appUpdateManager.startUpdateFlowForResult(
            info,
            updateType,
            activity,
            requestCode
        )
    }

    override fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
}

class InAppUpdateManager(
    private val activity: ComponentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val playUpdateManager: PlayUpdateManagerWrapper = PlayUpdateManagerWrapperImpl(activity)
) {
    private var listener: InstallStateUpdatedListener? = null
    private var updateType: Int = AppUpdateType.FLEXIBLE

    fun checkForUpdate() {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }

        playUpdateManager.getAppUpdateInfo(
            onSuccess = { info -> handleUpdateInfo(info) },
            onFailure = { e -> Log.e(TAG, "Failed to check for updates", e) }
        )
    }

    private fun handleUpdateInfo(info: UpdateInfo) {
        if (info.updateAvailability != UpdateAvailability.UPDATE_AVAILABLE) {
            return
        }

        when {
            info.isFlexibleAllowed -> {
                updateType = AppUpdateType.FLEXIBLE
                startFlexibleUpdate()
            }
            info.isImmediateAllowed -> {
                updateType = AppUpdateType.IMMEDIATE
                startImmediateUpdate()
            }
        }
    }

    private fun startFlexibleUpdate() {
        listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> showUpdateReadySnackbar()
                InstallStatus.FAILED -> Log.e(TAG, "Update download failed")
                else -> Unit
            }
        }

        listener?.let { playUpdateManager.registerListener(it) }

        playUpdateManager.startUpdateFlowForResult(
            UPDATE_REQUEST_CODE,
            AppUpdateType.FLEXIBLE
        )
    }

    private fun startImmediateUpdate() {
        playUpdateManager.startUpdateFlowForResult(
            UPDATE_REQUEST_CODE,
            AppUpdateType.IMMEDIATE
        )
    }

    private fun showUpdateReadySnackbar() {
        val rootView = activity.window?.decorView?.rootView ?: return
        Snackbar.make(
            rootView,
            activity.getString(R.string.update_ready_message),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(activity.getString(R.string.update_restart)) {
            playUpdateManager.completeUpdate()
        }.show()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Update flow failed. Result code: $resultCode")
        }
    }

    fun onResume() {
        playUpdateManager.getAppUpdateInfo(
            onSuccess = { info ->
                if (info.installStatus == InstallStatus.DOWNLOADED) {
                    showUpdateReadySnackbar()
                } else if (info.updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    playUpdateManager.startUpdateFlowForResult(
                        UPDATE_REQUEST_CODE,
                        updateType
                    )
                }
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to check update status on resume", e)
            }
        )
    }

    fun onDestroy() {
        listener?.let { playUpdateManager.unregisterListener(it) }
        listener = null
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
        const val UPDATE_REQUEST_CODE = 1001
    }
}
