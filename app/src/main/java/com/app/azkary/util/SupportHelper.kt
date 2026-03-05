package com.app.azkary.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.app.azkary.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupportHelper - Utility class for Support/Feedback functionality
 *
 * Provides methods to:
 * - Build device info block for support tickets
 * - Create email intents with pre-filled body
 * - Handle safe intent launching
 */
@Singleton
class SupportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localeManager: LocaleManager
) {

    companion object {
        private const val SUPPORT_EMAIL = "azkary@hearo.support"
        private const val DISCORD_INVITE_URL = "https://discord.gg/2Kb7scp48C"
    }

    /**
     * Data class containing device/app information for support
     */
    data class DeviceInfo(
        val appVersion: String,
        val buildNumber: Int,
        val deviceManufacturer: String,
        val deviceModel: String,
        val androidVersion: String,
        val androidSdk: Int,
        val locale: String,
        val timezone: String
    )

    /**
     * Get current device/app information
     */
    fun getDeviceInfo(): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentLocale = localeManager.getCurrentLocale(context)
        val currentTimezone = TimeZone.getDefault().id

        // Use longVersionCode on API 28+, fallback to versionCode for older versions
        val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }

        return DeviceInfo(
            appVersion = packageInfo.versionName ?: "unknown",
            buildNumber = buildNumber,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
            locale = "${currentLocale.language}-${currentLocale.country}",
            timezone = currentTimezone
        )
    }

    /**
     * Build email intent with pre-filled subject and body
     * Uses mailto URI with encoded query parameters for maximum compatibility
     * across different email apps (Gmail, Samsung Email, Outlook, etc.)
     */
    fun buildEmailIntent(): Intent {
        val deviceInfo = getDeviceInfo()

        val subject = context.getString(R.string.support_email_subject)
        val body = buildEmailBody(deviceInfo)

        // Encode subject and body into mailto URI query parameters
        // This is more reliable than using intent extras with ACTION_SENDTO
        val uri = buildString {
            append("mailto:")
            append(SUPPORT_EMAIL)
            append("?subject=")
            append(URLEncoder.encode(subject, "UTF-8"))
            append("&body=")
            append(URLEncoder.encode(body, "UTF-8"))
        }

        return Intent(Intent.ACTION_SENDTO, uri.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Build email body with localized labels and device info
     */
    private fun buildEmailBody(deviceInfo: DeviceInfo): String {
        return buildString {
            // Useful Info Block
            append(context.getString(R.string.support_useful_info_label))
            append("\n")
            append(context.getString(R.string.support_app_version_label))
            append(" ")
            append(deviceInfo.appVersion)
            append(" (")
            append(context.getString(R.string.support_build_label))
            append(" ")
            append(deviceInfo.buildNumber)
            append(")\n")
            append(context.getString(R.string.support_android_version_label))
            append(" ")
            append(deviceInfo.androidVersion)
            append("\n")
            append(context.getString(R.string.support_device_label))
            append(" ")
            append(deviceInfo.deviceManufacturer)
            append(" ")
            append(deviceInfo.deviceModel)
            append("\n")
            append(context.getString(R.string.support_locale_label))
            append(" ")
            append(deviceInfo.locale)
            append("\n")
            append(context.getString(R.string.support_timezone_label))
            append(" ")
            append(deviceInfo.timezone)
            append("\n")
            append("---\n")
            // Instruction line
            append(context.getString(R.string.support_write_message_instruction))
            append("\n\n")
        }
    }

    /**
     * Build Discord invite intent
     * Opens Discord app if installed, otherwise opens browser
     */
    fun buildDiscordIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, DISCORD_INVITE_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Safely launch an intent with error handling
     * Returns true if intent was launched successfully, false otherwise
     */
    fun launchIntentSafely(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get localized string resource
     */
    fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

}
