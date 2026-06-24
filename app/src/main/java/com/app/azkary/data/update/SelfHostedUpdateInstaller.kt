package com.app.azkary.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfHostedUpdateInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            android.net.Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun downloadAndOpenInstaller(update: SelfHostedUpdate): InstallLaunchResult {
        if (!canRequestPackageInstalls()) {
            return InstallLaunchResult.PermissionRequired
        }

        return runCatching {
            val apkFile = withContext(Dispatchers.IO) {
                downloadAndVerify(update)
            }
            openInstaller(apkFile)
            InstallLaunchResult.Started
        }.getOrElse { error ->
            InstallLaunchResult.Failed(error.message ?: "Unable to start update install")
        }
    }

    private fun downloadAndVerify(update: SelfHostedUpdate): File {
        require(update.apkUrl.isNotBlank()) { "Update download URL is missing" }

        val updatesDir = File(context.cacheDir, UPDATE_CACHE_DIR).apply { mkdirs() }
        val apkFile = File(updatesDir, "azkary-${update.latestVersionCode}.apk")
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val request = Request.Builder()
            .url(update.apkUrl)
            .build()
        val digest = MessageDigest.getInstance("SHA-256")

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Update download failed: HTTP ${response.code}")
            }

            response.body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
        }

        val expectedHash = update.sha256.trim()
        if (expectedHash.isNotEmpty()) {
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                apkFile.delete()
                error("Downloaded APK did not match the expected checksum")
            }
        }

        return apkFile
    }

    private fun openInstaller(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: ActivityNotFoundException) {
            throw IllegalStateException("No installer app is available on this device", e)
        }
    }

    private companion object {
        const val UPDATE_CACHE_DIR = "updates"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

sealed class InstallLaunchResult {
    data object Started : InstallLaunchResult()
    data object PermissionRequired : InstallLaunchResult()
    data class Failed(val message: String) : InstallLaunchResult()
}
