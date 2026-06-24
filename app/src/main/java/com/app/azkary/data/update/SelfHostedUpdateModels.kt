package com.app.azkary.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SelfHostedUpdateManifest(
    @SerialName("enabled")
    val enabled: Boolean = false,

    @SerialName("latestVersionCode")
    val latestVersionCode: Int = 0,

    @SerialName("latestVersionName")
    val latestVersionName: String = "",

    @SerialName("minSupportedVersionCode")
    val minSupportedVersionCode: Int = 0,

    @SerialName("apkUrl")
    val apkUrl: String = "",

    @SerialName("sha256")
    val sha256: String = "",

    @SerialName("mandatory")
    val mandatory: Boolean = false,

    @SerialName("releaseNotes")
    val releaseNotes: List<String> = emptyList()
)

data class SelfHostedUpdate(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minSupportedVersionCode: Int,
    val apkUrl: String,
    val sha256: String,
    val mandatory: Boolean,
    val releaseNotes: List<String>
)

sealed class SelfHostedUpdateCheckResult {
    data object UpToDate : SelfHostedUpdateCheckResult()
    data object NotConfigured : SelfHostedUpdateCheckResult()
    data class Available(val update: SelfHostedUpdate) : SelfHostedUpdateCheckResult()
}
