package com.example.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateManifest(
    val applicationName: String = "FAZA Player",
    @JsonNames("version", "latest_version", "versionName")
    val latestVersion: String = "",
    @JsonNames("versionCode", "latest_version_code", "version_code", "buildNumber")
    val latestVersionCode: Int = 0,
    val developerName: String = "Rajbir Singh",
    val companyName: String = "Gooin India Pvt Ltd",
    val releaseDate: String = "Latest Release",
    @JsonNames("url", "apkUrl", "download_url")
    val downloadUrl: String = "",
    val sourceLabel: String = "version.json",
    val newFeatures: List<String> = emptyList(),
    val performanceImprovements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList(),
    val versionHistory: List<VersionRelease> = emptyList()
)

@Serializable
data class VersionRelease(
    val version: String,
    val versionCode: Int,
    val releaseDate: String,
    val summary: String = "",
    val newFeatures: List<String> = emptyList(),
    val performanceImprovements: List<String> = emptyList(),
    val bugFixes: List<String> = emptyList(),
    val isInstalledBuild: Boolean = false
)

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

@Serializable
data class GitHubTagDto(
    val name: String = ""
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpdateAvailable(val manifest: UpdateManifest) : UpdateCheckState
    data class UpToDate(val checkedAtMillis: Long, val serverHistory: List<VersionRelease>) : UpdateCheckState
    data class Unavailable(val reason: String, val checkedAtMillis: Long) : UpdateCheckState
}
