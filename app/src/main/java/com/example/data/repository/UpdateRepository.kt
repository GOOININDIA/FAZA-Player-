package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.GitHubReleaseDto
import com.example.data.model.GitHubTagDto
import com.example.data.model.UpdateCheckState
import com.example.data.model.UpdateManifest
import com.example.data.model.VersionRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

data class GitHubRepoRef(
    val owner: String,
    val repo: String
) {
    val htmlUrl: String get() = "https://github.com/$owner/$repo"
    val releasesApiUrl: String get() = "https://api.github.com/repos/$owner/$repo/releases?per_page=15"
    val tagsApiUrl: String get() = "https://api.github.com/repos/$owner/$repo/tags?per_page=20"
}

class UpdateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val currentInstalledRelease = VersionRelease(
        version = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        releaseDate = "November 2026",
        summary = "What's New – v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) by Gooin India Pvt Ltd.",
        newFeatures = listOf(
            "Added an animated Profile header greeting ('Welcome, [User Name]') with spring entrance, expanding avatar halo, and subtle light sweep upon successful Google Sign-In for a smoother onboarding feel.",
            "Added a global AuthStateListener & IdTokenListener in the Profile ViewModel that updates Profile and FAZA AI UI state in real time whenever a session expires, a token is revoked, or a user logs out.",
            "Added a dedicated Profile error boundary banner and Material 3 snackbar notification system for Google Sign-In failures, network timeouts, and session expirations with one-tap Retry / Sign In recovery and dismiss actions.",
            "Added Sign in with Google inside the Profile tab using Jetpack Credential Manager & Firebase Authentication, linking each user's unique Google account ID (sub) to FAZA AI Chat with profile display, sign-out controls, persistent sessions, and strict per-account isolated chat history.",
            "Redesigned Playing UI Gesture Controls with a completely transparent, background-free minimalist design for Brightness, Volume, Seek, Double-Tap Skip, Zoom, and Hold-Speed indicators with smooth fade transitions and crisp legibility over bright and dark video scenes.",
            "Added a Dynamic Custom Theme Color System in Settings → Theme → Custom Color with 11 preset themes (Blue, Purple, Green, Red, Orange, Pink, Teal, Cyan, Yellow, Deep Purple, Monochrome), an interactive 2D spectrum & HEX color picker with live preview, and instant propagation across the entire app in Light, Dark, and System Default modes.",
            "Added AppUpdateManager with periodic Firebase Remote Config version checking, top-of-screen 'Update Available' banner, and a dedicated Updates & Changelog screen with direct download/update flow.",
            "Upgraded all cards across Home, Favorites, Profile, Settings, About, and Updates to the emerald-sweep card design with bottom-right ambient glow and animated press/hover sweep.",
            "Added a custom animated multi-ring loader for Video Player buffering, Free Access rewarded ad loading, and the Update checker (Updates screen and update banner).",
            "Updated the official FAZA Player application icon across all Android densities (MDPI–XXXHDPI), adaptive foreground/background/monochrome layers, playback notifications, and in-app branding.",
            "Added adaptive Google AdMob banner ads to the Favorites tab, automatically respecting offline mode and 24-hour Free Access.",
            "New Favorites tab in the main navigation bar to bookmark, sort, manage, and quickly play your preferred video files.",
            "Complete Material You Expressive redesign across Home folders, Favorites, Search, Profile, Settings, Updates, Dialogs, and Player sheets.",
            "Replaced legacy glass borders and gradients with clean borderless tonal containers, 24–28dp rounded shapes, and full-pill buttons & chips.",
            "Responsive spring bounce animations across the navigation dock, folder cards, theme tiles, and rounded FAZA UI player controls."
        ),
        performanceImprovements = listOf(
            "Pure tonal M3 surface elevation eliminates costly blur and gradient overdraw for smoother 60–120fps scrolling.",
            "Optimized video folder scanning, thumbnail caching, and folder-scoped playback queue navigation."
        ),
        bugFixes = listOf(
            "Preserved native Android device font and adaptive System UI contrast across light and dark modes.",
            "Improved overall stability, touch target ergonomics, and layout consistency across phones and tablets."
        ),
        isInstalledBuild = true
    )

    suspend fun checkForUpdates(manifestUrl: String): UpdateCheckState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val trimmedUrl = manifestUrl.trim()
        if (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://")) {
            return@withContext UpdateCheckState.Unavailable(
                reason = "Invalid update server URL configured.",
                checkedAtMillis = now
            )
        }

        val gitHubRepo = parseGitHubRepoUrl(trimmedUrl)
        if (gitHubRepo != null) {
            return@withContext checkGitHubRepository(gitHubRepo, now)
        }

        try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("User-Agent", "FAZAPlayer/${BuildConfig.VERSION_NAME} (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckState.Unavailable(
                        reason = "Update server returned HTTP ${response.code}.",
                        checkedAtMillis = now
                    )
                }
                val body = response.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) {
                    return@withContext UpdateCheckState.Unavailable(
                        reason = "Empty update manifest received.",
                        checkedAtMillis = now
                    )
                }

                val manifest = parseManifest(body)
                if (manifest.latestVersion.isBlank() && manifest.latestVersionCode <= 0) {
                    return@withContext UpdateCheckState.Unavailable(
                        reason = "Remote version.json missing version fields.",
                        checkedAtMillis = now
                    )
                }

                if (isUpdateAvailable(manifest)) {
                    UpdateCheckState.UpdateAvailable(manifest)
                } else {
                    UpdateCheckState.UpToDate(
                        checkedAtMillis = now,
                        serverHistory = manifest.versionHistory
                    )
                }
            }
        } catch (e: Exception) {
            UpdateCheckState.Unavailable(
                reason = e.localizedMessage ?: "Unable to reach update server.",
                checkedAtMillis = now
            )
        }
    }

    private fun checkGitHubRepository(repo: GitHubRepoRef, now: Long): UpdateCheckState {
        return try {
            val releasesResult = fetchGitHubJson(repo.releasesApiUrl)
            val tagsResult = fetchGitHubJson(repo.tagsApiUrl)

            if (releasesResult.statusCode == 403 || releasesResult.statusCode == 429 ||
                tagsResult.statusCode == 403 || tagsResult.statusCode == 429
            ) {
                if (releasesResult.body.isNullOrBlank() && tagsResult.body.isNullOrBlank()) {
                    return UpdateCheckState.Unavailable(
                        reason = "GitHub API rate limit reached. Try again later.",
                        checkedAtMillis = now
                    )
                }
            }

            if (releasesResult.body == null && tagsResult.body == null) {
                val errCode = tagsResult.statusCode ?: releasesResult.statusCode
                return UpdateCheckState.Unavailable(
                    reason = if (errCode != null) {
                        "GitHub update check returned HTTP $errCode."
                    } else {
                        "Unable to reach GitHub repository (${repo.owner}/${repo.repo})."
                    },
                    checkedAtMillis = now
                )
            }

            resolveGitHubUpdateState(
                repo = repo,
                releasesJson = releasesResult.body,
                tagsJson = tagsResult.body,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME,
                checkedAtMillis = now
            )
        } catch (e: Exception) {
            UpdateCheckState.Unavailable(
                reason = e.localizedMessage ?: "Unable to reach GitHub update host.",
                checkedAtMillis = now
            )
        }
    }

    private data class HttpFetchResult(
        val statusCode: Int?,
        val body: String?
    )

    private fun fetchGitHubJson(url: String): HttpFetchResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "FAZAPlayer/${BuildConfig.VERSION_NAME} (Android)")
                .build()
            client.newCall(request).execute().use { response ->
                val raw = if (response.isSuccessful) response.body?.string()?.trim() else null
                HttpFetchResult(
                    statusCode = response.code,
                    body = raw?.takeIf { it.isNotBlank() }
                )
            }
        } catch (_: Exception) {
            HttpFetchResult(statusCode = null, body = null)
        }
    }

    internal fun resolveGitHubUpdateState(
        repo: GitHubRepoRef,
        releasesJson: String?,
        tagsJson: String?,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        checkedAtMillis: Long = System.currentTimeMillis()
    ): UpdateCheckState {
        val parsedReleases = parseGitHubReleases(releasesJson)
            .filter { !it.draft && (it.tagName.isNotBlank() || it.name.isNotBlank()) }
        val parsedTags = parseGitHubTags(tagsJson)
            .filter { it.name.isNotBlank() }

        val candidatesByKey = linkedMapOf<String, GitHubCandidate>()

        for (release in parsedReleases) {
            val rawTag = release.tagName.ifBlank { release.name }.trim()
            val key = normalizeVersionKey(rawTag)
            val apkAsset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
            }
            val targetUrl = apkAsset?.browserDownloadUrl
                ?: release.htmlUrl.ifBlank { "${repo.htmlUrl}/releases/tag/$rawTag" }
            val releaseDate = formatGitHubDate(release.publishedAt)
                .ifBlank { if (release.prerelease) "Pre-release" else "GitHub Release" }
            val notes = parseReleaseBullets(
                body = release.body,
                fallbackTitle = release.name,
                tagName = rawTag,
                repo = repo
            )
            val sourceLabel = if (release.prerelease) "GitHub Beta" else "GitHub Release"
            val summary = release.name.trim().takeIf { it.isNotBlank() && !it.equals(rawTag, ignoreCase = true) }
                ?: "Published via ${repo.owner}/${repo.repo} ($sourceLabel)."

            candidatesByKey[key] = GitHubCandidate(
                rawTag = rawTag,
                displayVersion = normalizeVersionDisplay(rawTag),
                releaseDate = releaseDate,
                downloadUrl = targetUrl,
                sourceLabel = sourceLabel,
                summary = summary,
                notes = notes
            )
        }

        for (tag in parsedTags) {
            val rawTag = tag.name.trim()
            val key = normalizeVersionKey(rawTag)
            if (key !in candidatesByKey) {
                candidatesByKey[key] = GitHubCandidate(
                    rawTag = rawTag,
                    displayVersion = normalizeVersionDisplay(rawTag),
                    releaseDate = "GitHub Tag",
                    downloadUrl = "${repo.htmlUrl}/releases/tag/$rawTag",
                    sourceLabel = "GitHub Tag",
                    summary = "Git tag $rawTag on ${repo.owner}/${repo.repo}.",
                    notes = listOf("New version tag $rawTag detected on GitHub (${repo.owner}/${repo.repo})")
                )
            }
        }

        val sortedCandidates = candidatesByKey.values.sortedWith { a, b ->
            compareVersions(b.rawTag, a.rawTag)
        }

        val history = sortedCandidates.take(12).map { candidate ->
            VersionRelease(
                version = candidate.displayVersion,
                versionCode = 0,
                releaseDate = candidate.releaseDate,
                summary = candidate.summary,
                newFeatures = candidate.notes,
                isInstalledBuild = false
            )
        }

        val newest = sortedCandidates.firstOrNull()
        if (newest != null && isVersionNewer(newest.rawTag, currentVersionName)) {
            val manifest = UpdateManifest(
                applicationName = "FAZA Player",
                latestVersion = newest.displayVersion,
                latestVersionCode = 0,
                developerName = "Rajbir Singh",
                companyName = "Gooin India Pvt Ltd",
                releaseDate = newest.releaseDate,
                downloadUrl = newest.downloadUrl,
                sourceLabel = newest.sourceLabel,
                newFeatures = newest.notes,
                versionHistory = history
            )
            return UpdateCheckState.UpdateAvailable(manifest)
        }

        return UpdateCheckState.UpToDate(
            checkedAtMillis = checkedAtMillis,
            serverHistory = history
        )
    }

    private data class GitHubCandidate(
        val rawTag: String,
        val displayVersion: String,
        val releaseDate: String,
        val downloadUrl: String,
        val sourceLabel: String,
        val summary: String,
        val notes: List<String>
    )

    internal fun parseGitHubReleases(rawJson: String?): List<GitHubReleaseDto> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<GitHubReleaseDto>>(rawJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parseGitHubTags(rawJson: String?): List<GitHubTagDto> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<GitHubTagDto>>(rawJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parseGitHubRepoUrl(rawUrl: String): GitHubRepoRef? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.endsWith(".json", ignoreCase = true)) return null
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase() ?: return null
        val segments = uri.path.orEmpty()
            .split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return when {
            (host == "github.com" || host == "www.github.com") && segments.size >= 2 -> {
                val owner = segments[0]
                val repo = segments[1].removeSuffix(".git").trimEnd('-')
                if (owner.isNotBlank() && repo.isNotBlank()) GitHubRepoRef(owner, repo) else null
            }
            host == "api.github.com" && segments.size >= 3 && segments[0] == "repos" -> {
                val owner = segments[1]
                val repo = segments[2].removeSuffix(".git").trimEnd('-')
                if (owner.isNotBlank() && repo.isNotBlank()) GitHubRepoRef(owner, repo) else null
            }
            else -> null
        }
    }

    internal fun parseManifest(rawJson: String): UpdateManifest =
        json.decodeFromString<UpdateManifest>(rawJson)

    internal fun isUpdateAvailable(
        manifest: UpdateManifest,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Boolean {
        val codeNewer = manifest.latestVersionCode > currentVersionCode
        val nameNewer = manifest.latestVersion.isNotBlank() &&
            isVersionNewer(manifest.latestVersion, currentVersionName)
        return codeNewer || nameNewer
    }

    internal fun isVersionNewer(candidate: String, current: String): Boolean {
        return compareVersions(candidate, current) > 0
    }

    internal fun compareVersions(candidate: String, current: String): Int {
        val left = parseVersionTokens(candidate) ?: return -1
        val right = parseVersionTokens(current) ?: return 1

        val maxLen = maxOf(left.numbers.size, right.numbers.size)
        for (i in 0 until maxLen) {
            val l = left.numbers.getOrElse(i) { 0 }
            val r = right.numbers.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }

        if (left.stageRank != right.stageRank) {
            return left.stageRank.compareTo(right.stageRank)
        }
        return left.stageNumber.compareTo(right.stageNumber)
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val stageRank: Int,
        val stageNumber: Int
    )

    private fun parseVersionTokens(raw: String): ParsedVersion? {
        val match = VERSION_REGEX.find(raw.trim()) ?: return null
        val numbers = match.groupValues[1]
            .split(".")
            .mapNotNull { it.toIntOrNull() }
        if (numbers.isEmpty()) return null

        val suffix = match.groupValues.getOrNull(2)?.trim()?.lowercase().orEmpty()
        val (stageRank, stageNum) = when {
            suffix.isEmpty() -> 100 to 0
            suffix.contains("rc") -> 3 to (TRAILING_INT_REGEX.find(suffix)?.value?.toIntOrNull() ?: 0)
            suffix.contains("beta") || suffix.contains("preview") ->
                2 to (TRAILING_INT_REGEX.find(suffix)?.value?.toIntOrNull() ?: 0)
            suffix.contains("alpha") || suffix.contains("dev") ->
                1 to (TRAILING_INT_REGEX.find(suffix)?.value?.toIntOrNull() ?: 0)
            else -> 50 to (TRAILING_INT_REGEX.find(suffix)?.value?.toIntOrNull() ?: 0)
        }
        return ParsedVersion(numbers = numbers, stageRank = stageRank, stageNumber = stageNum)
    }

    private fun normalizeVersionKey(rawTag: String): String =
        rawTag.trim().removePrefix("v").removePrefix("V").lowercase()

    private fun normalizeVersionDisplay(rawTag: String): String =
        rawTag.trim().removePrefix("v").removePrefix("V")

    private fun formatGitHubDate(isoTimestamp: String): String {
        val trimmed = isoTimestamp.trim()
        return if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            trimmed.substring(0, 10)
        } else {
            ""
        }
    }

    private fun parseReleaseBullets(
        body: String,
        fallbackTitle: String,
        tagName: String,
        repo: GitHubRepoRef
    ): List<String> {
        val lines = body.lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("• ")
                    .replace(Regex("^#+\\s*"), "")
                    .trim()
            }
            .filter { it.isNotBlank() && !it.startsWith("<!--") }
            .take(6)
            .toList()
        if (lines.isNotEmpty()) return lines
        val cleanTitle = fallbackTitle.trim()
        return if (cleanTitle.isNotBlank() && !cleanTitle.equals(tagName, ignoreCase = true)) {
            listOf("$cleanTitle (${repo.owner}/${repo.repo})")
        } else {
            listOf("Tag $tagName published on ${repo.owner}/${repo.repo}")
        }
    }

    companion object {
        private val VERSION_REGEX = Regex(".*?(\\d+(?:\\.\\d+)*)(?:[\\s._-]*(.+))?$")
        private val TRAILING_INT_REGEX = Regex("(\\d+)")
    }
}
