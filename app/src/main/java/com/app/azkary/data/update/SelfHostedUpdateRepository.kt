package com.app.azkary.data.update

import com.app.azkary.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfHostedUpdateRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    suspend fun checkForUpdate(): Result<SelfHostedUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val manifestUrl = BuildConfig.SELF_HOSTED_UPDATE_MANIFEST_URL.trim()
            if (manifestUrl.isBlank()) {
                return@runCatching SelfHostedUpdateCheckResult.NotConfigured
            }

            val request = Request.Builder()
                .url(manifestUrl)
                .header("Cache-Control", "no-cache")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Update check failed: HTTP ${response.code}")
                }

                val body = response.body.string()
                val manifest = json.decodeFromString<SelfHostedUpdateManifest>(body)
                manifest.toCheckResult()
            }
        }
    }

    private fun SelfHostedUpdateManifest.toCheckResult(): SelfHostedUpdateCheckResult {
        if (!enabled) {
            return SelfHostedUpdateCheckResult.NotConfigured
        }

        if (latestVersionCode <= BuildConfig.VERSION_CODE) {
            return SelfHostedUpdateCheckResult.UpToDate
        }

        require(apkUrl.isNotBlank()) { "Update APK URL is missing" }

        return SelfHostedUpdateCheckResult.Available(
            SelfHostedUpdate(
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName.ifBlank { latestVersionCode.toString() },
                minSupportedVersionCode = minSupportedVersionCode,
                apkUrl = apkUrl,
                sha256 = sha256,
                mandatory = mandatory || BuildConfig.VERSION_CODE < minSupportedVersionCode,
                releaseNotes = releaseNotes.filter { it.isNotBlank() }
            )
        )
    }
}
