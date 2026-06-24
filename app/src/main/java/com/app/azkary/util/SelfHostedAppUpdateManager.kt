package com.app.azkary.util

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.app.azkary.BuildConfig
import com.app.azkary.R
import com.app.azkary.data.update.InstallLaunchResult
import com.app.azkary.data.update.SelfHostedUpdate
import com.app.azkary.data.update.SelfHostedUpdateCheckResult
import com.app.azkary.data.update.SelfHostedUpdateInstaller
import com.app.azkary.data.update.SelfHostedUpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SelfHostedAppUpdateManager(
    private val activity: ComponentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val repository: SelfHostedUpdateRepository,
    private val installer: SelfHostedUpdateInstaller
) : AppUpdateManager {

    private var checkJob: Job? = null
    private var installJob: Job? = null
    private var dialog: AlertDialog? = null
    private var activeUpdate: SelfHostedUpdate? = null
    private var dismissedVersionCode: Int? = null

    override fun checkForUpdate() {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        if (checkJob?.isActive == true || installJob?.isActive == true || dialog?.isShowing == true) {
            return
        }

        checkJob = activity.lifecycleScope.launch {
            repository.checkForUpdate()
                .onSuccess { result ->
                    when (result) {
                        is SelfHostedUpdateCheckResult.Available -> handleAvailableUpdate(result.update)
                        SelfHostedUpdateCheckResult.NotConfigured,
                        SelfHostedUpdateCheckResult.UpToDate -> {
                            activeUpdate = null
                        }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to check self-hosted updates", error)
                }
        }
    }

    private fun handleAvailableUpdate(update: SelfHostedUpdate) {
        if (!update.mandatory && dismissedVersionCode == update.latestVersionCode) {
            return
        }

        activeUpdate = update
        showUpdateDialog(update)
    }

    private fun showUpdateDialog(update: SelfHostedUpdate, errorMessage: String? = null) {
        activity.runOnUiThread {
            dismissDialog()

            val newDialog = AlertDialog.Builder(activity)
                .setTitle(
                    if (update.mandatory) {
                        R.string.update_required_title
                    } else {
                        R.string.update_available_title
                    }
                )
                .setMessage(buildUpdateMessage(update, errorMessage))
                .setPositiveButton(R.string.update_button, null)
                .apply {
                    if (!update.mandatory) {
                        setNegativeButton(R.string.update_later) { _, _ ->
                            dismissedVersionCode = update.latestVersionCode
                            activeUpdate = null
                        }
                    }
                }
                .create()

            newDialog.setCancelable(!update.mandatory)
            newDialog.setOnShowListener {
                newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    downloadAndInstall(update)
                }
            }
            dialog = newDialog
            newDialog.show()
        }
    }

    private fun buildUpdateMessage(update: SelfHostedUpdate, errorMessage: String? = null): String {
        return buildString {
            appendLine(
                activity.getString(
                    R.string.update_available_message,
                    update.latestVersionName,
                    BuildConfig.VERSION_NAME
                )
            )

            if (update.mandatory) {
                appendLine()
                appendLine(activity.getString(R.string.update_required_message))
            }

            if (update.releaseNotes.isNotEmpty()) {
                appendLine()
                appendLine(activity.getString(R.string.update_whats_new))
                update.releaseNotes.take(MAX_VISIBLE_RELEASE_NOTES).forEach { note ->
                    appendLine("- $note")
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                appendLine()
                append(activity.getString(R.string.update_error_message, errorMessage))
            }
        }.trim()
    }

    private fun downloadAndInstall(update: SelfHostedUpdate) {
        if (installJob?.isActive == true) return

        dialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
        dialog?.setMessage(buildUpdateMessage(update, activity.getString(R.string.update_downloading)))

        installJob = activity.lifecycleScope.launch {
            when (val result = installer.downloadAndOpenInstaller(update)) {
                InstallLaunchResult.Started -> {
                    dismissDialog()
                    Toast.makeText(
                        activity,
                        R.string.update_installer_opened,
                        Toast.LENGTH_LONG
                    ).show()
                }
                InstallLaunchResult.PermissionRequired -> showInstallPermissionDialog(update)
                is InstallLaunchResult.Failed -> showUpdateDialog(update, result.message)
            }
        }
    }

    private fun showInstallPermissionDialog(update: SelfHostedUpdate) {
        activity.runOnUiThread {
            dismissDialog()

            val newDialog = AlertDialog.Builder(activity)
                .setTitle(R.string.update_install_permission_title)
                .setMessage(R.string.update_install_permission_message)
                .setPositiveButton(R.string.update_open_settings) { _, _ ->
                    installer.openInstallPermissionSettings()
                }
                .apply {
                    if (!update.mandatory) {
                        setNegativeButton(R.string.update_later) { _, _ ->
                            dismissedVersionCode = update.latestVersionCode
                            activeUpdate = null
                        }
                    }
                }
                .create()

            newDialog.setCancelable(!update.mandatory)
            dialog = newDialog
            newDialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int) = Unit

    override fun onResume() {
        val update = activeUpdate
        if (update != null && dialog?.isShowing != true) {
            showUpdateDialog(update)
        } else {
            checkForUpdate()
        }
    }

    override fun onDestroy() {
        checkJob?.cancel()
        installJob?.cancel()
        dismissDialog()
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    private companion object {
        const val TAG = "SelfHostedUpdateManager"
        const val MAX_VISIBLE_RELEASE_NOTES = 5
    }
}
