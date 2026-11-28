package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.UpdateCheckState
import com.example.data.model.UpdateManifest
import com.example.data.model.UserPreferences
import com.example.data.model.VersionRelease
import com.example.data.repository.UpdateRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Snapshot of version-update parameters fetched from Firebase Remote Config.
 */
data class RemoteConfigUpdateSnapshot(
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val downloadUrl: String = "",
    val releaseDate: String = "",
    val summary: String = "",
    val changelogRaw: String = "",
    val performanceRaw: String = "",
    val bugFixesRaw: String = "",
    val isMandatory: Boolean = false,
    val manifestJsonRaw: String = ""
)

/**
 * Abstraction over Firebase Remote Config to keep [AppUpdateManager] testable in unit and UI tests.
 */
interface RemoteConfigUpdateSource {
    suspend fun fetchLatestConfig(forceRefresh: Boolean): Result<RemoteConfigUpdateSnapshot>
    fun startRealtimeListener(onConfigUpdated: () -> Unit) {}
    fun stopRealtimeListener() {}
}

/**
 * Production [RemoteConfigUpdateSource] backed by Firebase Remote Config.
 */
class FirebaseRemoteConfigUpdateSource(
    private val context: Context
) : RemoteConfigUpdateSource {

    @Volatile
    private var defaultsApplied = false

    @Volatile
    private var realtimeRegistration: ConfigUpdateListenerRegistration? = null

    private fun getRemoteConfigOrNull(): FirebaseRemoteConfig? {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isEmpty()) {
                null
            } else {
                FirebaseRemoteConfig.getInstance()
            }
        }.onFailure { err ->
            Log.w(TAG, "Firebase Remote Config unavailable: ${err.message}")
        }.getOrNull()
    }

    override suspend fun fetchLatestConfig(forceRefresh: Boolean): Result<RemoteConfigUpdateSnapshot> =
        withContext(Dispatchers.IO) {
            val remoteConfig = getRemoteConfigOrNull()
                ?: return@withContext Result.failure(
                    IllegalStateException("Firebase Remote Config is not initialized.")
                )

            try {
                val minFetchIntervalSeconds = when {
                    forceRefresh -> 0L
                    BuildConfig.DEBUG -> DEBUG_MIN_FETCH_INTERVAL_SECONDS
                    else -> RELEASE_MIN_FETCH_INTERVAL_SECONDS
                }

                val settings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minFetchIntervalSeconds)
                    .setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS)
                    .build()
                remoteConfig.setConfigSettingsAsync(settings).awaitTask()

                if (!defaultsApplied) {
                    remoteConfig.setDefaultsAsync(AppUpdateManager.defaultRemoteConfigValues()).awaitTask()
                    defaultsApplied = true
                }

                remoteConfig.fetch(minFetchIntervalSeconds).awaitTask()
                remoteConfig.activate().awaitTask()

                Result.success(readSnapshot(remoteConfig))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (err: Exception) {
                Log.w(TAG, "Remote Config fetch failed, using activated values if any: ${err.message}")
                Result.failure(err)
            }
        }

    override fun startRealtimeListener(onConfigUpdated: () -> Unit) {
        if (realtimeRegistration != null) return
        val remoteConfig = getRemoteConfigOrNull() ?: return
        runCatching {
            realtimeRegistration = remoteConfig.addOnConfigUpdateListener(
                object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate: ConfigUpdate) {
                        val relevantKeys = AppUpdateManager.ALL_REMOTE_CONFIG_KEYS
                        if (configUpdate.updatedKeys.any { it in relevantKeys }) {
                            remoteConfig.activate().addOnCompleteListener {
                                onConfigUpdated()
                            }
                        }
                    }

                    override fun onError(error: FirebaseRemoteConfigException) {
                        Log.w(TAG, "Remote Config real-time update error: ${error.message}")
                    }
                }
            )
        }.onFailure { err ->
            Log.w(TAG, "Unable to attach Remote Config listener: ${err.message}")
        }
    }

    override fun stopRealtimeListener() {
        runCatching { realtimeRegistration?.remove() }
        realtimeRegistration = null
    }

    private fun readSnapshot(remoteConfig: FirebaseRemoteConfig): RemoteConfigUpdateSnapshot {
        return RemoteConfigUpdateSnapshot(
            latestVersionName = remoteConfig.getString(AppUpdateManager.KEY_LATEST_VERSION_NAME).trim(),
            latestVersionCode = remoteConfig.getLong(AppUpdateManager.KEY_LATEST_VERSION_CODE).toInt(),
            downloadUrl = remoteConfig.getString(AppUpdateManager.KEY_DOWNLOAD_URL).trim(),
            releaseDate = remoteConfig.getString(AppUpdateManager.KEY_RELEASE_DATE).trim(),
            summary = remoteConfig.getString(AppUpdateManager.KEY_SUMMARY).trim(),
            changelogRaw = remoteConfig.getString(AppUpdateManager.KEY_CHANGELOG).trim(),
            performanceRaw = remoteConfig.getString(AppUpdateManager.KEY_PERFORMANCE_IMPROVEMENTS).trim(),
            bugFixesRaw = remoteConfig.getString(AppUpdateManager.KEY_BUG_FIXES).trim(),
            isMandatory = remoteConfig.getBoolean(AppUpdateManager.KEY_IS_MANDATORY),
            manifestJsonRaw = remoteConfig.getString(AppUpdateManager.KEY_MANIFEST_JSON).trim()
        )
    }

    companion object {
        private const val TAG = "FirebaseRemoteConfigSrc"
        private const val DEBUG_MIN_FETCH_INTERVAL_SECONDS = 60L
        private const val RELEASE_MIN_FETCH_INTERVAL_SECONDS = 3_600L
        private const val FETCH_TIMEOUT_SECONDS = 10L
    }
}

/**
 * Periodically checks for new FAZA Player releases using Firebase Remote Config,
 * parses release changelogs, exposes reactive update state, and safely launches
 * the update download flow.
 */
class AppUpdateManager(
    context: Context?,
    private val updateRepository: UpdateRepository,
    private val remoteConfigSource: RemoteConfigUpdateSource? =
        context?.let { FirebaseRemoteConfigUpdateSource(it.applicationContext) }
) {
    private val checkMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _lastCheckedAtMillis = MutableStateFlow<Long?>(null)
    val lastCheckedAtMillis: StateFlow<Long?> = _lastCheckedAtMillis.asStateFlow()

    private val _isPeriodicCheckActive = MutableStateFlow(false)
    val isPeriodicCheckActive: StateFlow<Boolean> = _isPeriodicCheckActive.asStateFlow()

    @Volatile
    private var periodicJob: Job? = null

    @Volatile
    private var configuredIntervalMs: Long = DEFAULT_PERIODIC_INTERVAL_MS

    val periodicIntervalMinutes: Int
        get() = (configuredIntervalMs / 60_000L).coerceAtLeast(1L).toInt()

    private fun activeRemoteConfigSource(): RemoteConfigUpdateSource? =
        testRemoteConfigSourceOverride ?: remoteConfigSource

    /**
     * Begins periodic Firebase Remote Config update polling and attaches a real-time
     * Remote Config listener so published console changes surface promptly.
     */
    fun startPeriodicChecks(
        scope: CoroutineScope,
        intervalMs: Long = DEFAULT_PERIODIC_INTERVAL_MS,
        fallbackManifestUrlProvider: suspend () -> String? = { null }
    ) {
        val safeIntervalMs = intervalMs.coerceAtLeast(MIN_PERIODIC_INTERVAL_MS)
        configuredIntervalMs = safeIntervalMs
        periodicJob?.cancel()

        val source = activeRemoteConfigSource()
        source?.startRealtimeListener {
            scope.launch {
                checkForUpdates(
                    forceRefresh = false,
                    fallbackManifestUrl = fallbackManifestUrlProvider()
                )
            }
        }

        _isPeriodicCheckActive.value = true
        periodicJob = scope.launch {
            try {
                while (isActive) {
                    checkForUpdates(
                        forceRefresh = false,
                        fallbackManifestUrl = fallbackManifestUrlProvider()
                    )
                    delay(safeIntervalMs)
                }
            } finally {
                _isPeriodicCheckActive.value = false
            }
        }
    }

    fun stopPeriodicChecks() {
        periodicJob?.cancel()
        periodicJob = null
        _isPeriodicCheckActive.value = false
        activeRemoteConfigSource()?.stopRealtimeListener()
    }

    /**
     * Performs a freshness check if enough time has elapsed since the previous check.
     */
    suspend fun checkIfStale(
        minIntervalMs: Long = DEFAULT_RESUME_STALE_INTERVAL_MS,
        fallbackManifestUrl: String? = null
    ): UpdateCheckState {
        val lastCheck = _lastCheckedAtMillis.value ?: 0L
        val now = System.currentTimeMillis()
        if (lastCheck > 0L && (now - lastCheck) < minIntervalMs && _updateState.value !is UpdateCheckState.Idle) {
            return _updateState.value
        }
        return checkForUpdates(
            forceRefresh = false,
            fallbackManifestUrl = fallbackManifestUrl
        )
    }

    /**
     * Checks Firebase Remote Config for a newer app version, falling back to the
     * configured GitHub/JSON manifest URL if Remote Config has no newer release or is unreachable.
     */
    suspend fun checkForUpdates(
        forceRefresh: Boolean = false,
        fallbackManifestUrl: String? = null,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): UpdateCheckState = checkMutex.withLock {
        _updateState.value = UpdateCheckState.Checking
        val checkedAt = System.currentTimeMillis()
        val source = activeRemoteConfigSource()

        val remoteResult = if (source != null) {
            source.fetchLatestConfig(forceRefresh = forceRefresh)
        } else {
            Result.failure(IllegalStateException("Remote Config source is unavailable."))
        }

        val finalState: UpdateCheckState = if (remoteResult.isSuccess) {
            val snapshot = remoteResult.getOrThrow()
            val evaluated = evaluateSnapshot(
                snapshot = snapshot,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName,
                checkedAtMillis = checkedAt
            )
            when (evaluated) {
                is UpdateCheckState.UpdateAvailable -> evaluated
                is UpdateCheckState.UpToDate -> {
                    // If Remote Config is at defaults and a fallback URL is set, check fallback for updates.
                    val fallbackUrl = fallbackManifestUrl?.trim().orEmpty()
                    if (fallbackUrl.isNotEmpty() && isUsingLocalDefaultSnapshot(snapshot, currentVersionCode, currentVersionName)) {
                        val fallbackState = updateRepository.checkForUpdates(fallbackUrl)
                        if (fallbackState is UpdateCheckState.UpdateAvailable) {
                            fallbackState
                        } else if (fallbackState is UpdateCheckState.UpToDate && fallbackState.serverHistory.isNotEmpty()) {
                            evaluated.copy(serverHistory = fallbackState.serverHistory)
                        } else {
                            evaluated
                        }
                    } else {
                        evaluated
                    }
                }
                else -> evaluated
            }
        } else {
            val fallbackUrl = fallbackManifestUrl?.trim().orEmpty()
            if (fallbackUrl.isNotEmpty()) {
                val fallbackState = updateRepository.checkForUpdates(fallbackUrl)
                if (fallbackState !is UpdateCheckState.Unavailable) {
                    fallbackState
                } else {
                    UpdateCheckState.Unavailable(
                        reason = remoteResult.exceptionOrNull()?.localizedMessage
                            ?: fallbackState.reason,
                        checkedAtMillis = checkedAt
                    )
                }
            } else {
                UpdateCheckState.Unavailable(
                    reason = remoteResult.exceptionOrNull()?.localizedMessage
                        ?: "Unable to fetch Firebase Remote Config.",
                    checkedAtMillis = checkedAt
                )
            }
        }

        _lastCheckedAtMillis.value = checkedAt
        _updateState.value = finalState
        finalState
    }

    internal fun evaluateSnapshot(
        snapshot: RemoteConfigUpdateSnapshot,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        checkedAtMillis: Long = System.currentTimeMillis()
    ): UpdateCheckState {
        val manifestFromJson = snapshot.manifestJsonRaw
            .takeIf { it.isNotBlank() }
            ?.let { raw ->
                runCatching { updateRepository.parseManifest(raw) }.getOrNull()
            }

        val resolvedVersionName = snapshot.latestVersionName
            .ifBlank { manifestFromJson?.latestVersion.orEmpty() }
            .trim()
        val resolvedVersionCode = if (snapshot.latestVersionCode > 0) {
            snapshot.latestVersionCode
        } else {
            manifestFromJson?.latestVersionCode ?: 0
        }

        val resolvedDownloadUrl = snapshot.downloadUrl
            .ifBlank { manifestFromJson?.downloadUrl.orEmpty() }
            .ifBlank { DEFAULT_DOWNLOAD_URL }
            .trim()

        val resolvedReleaseDate = snapshot.releaseDate
            .ifBlank { manifestFromJson?.releaseDate.orEmpty() }
            .ifBlank { "Latest Release" }
            .trim()

        val resolvedSummary = snapshot.summary
            .ifBlank { manifestFromJson?.summary.orEmpty() }
            .trim()

        val parsedFeatures = parseChangelogEntries(snapshot.changelogRaw)
            .ifEmpty { manifestFromJson?.newFeatures.orEmpty() }
        val parsedPerformance = parseChangelogEntries(snapshot.performanceRaw)
            .ifEmpty { manifestFromJson?.performanceImprovements.orEmpty() }
        val parsedBugFixes = parseChangelogEntries(snapshot.bugFixesRaw)
            .ifEmpty { manifestFromJson?.bugFixes.orEmpty() }

        val resolvedFeatures = if (parsedFeatures.isEmpty() && parsedPerformance.isEmpty() && parsedBugFixes.isEmpty() && resolvedVersionName.isNotBlank()) {
            listOf("FAZA Player v$resolvedVersionName is now available with the latest improvements and stability fixes.")
        } else {
            parsedFeatures
        }

        val manifest = UpdateManifest(
            applicationName = manifestFromJson?.applicationName?.ifBlank { "FAZA Player" } ?: "FAZA Player",
            latestVersion = resolvedVersionName,
            latestVersionCode = resolvedVersionCode,
            developerName = manifestFromJson?.developerName?.ifBlank { "Rajbir Singh" } ?: "Rajbir Singh",
            companyName = manifestFromJson?.companyName?.ifBlank { "Gooin India Pvt Ltd" } ?: "Gooin India Pvt Ltd",
            releaseDate = resolvedReleaseDate,
            downloadUrl = resolvedDownloadUrl,
            sourceLabel = SOURCE_LABEL_REMOTE_CONFIG,
            summary = resolvedSummary,
            isMandatory = snapshot.isMandatory || (manifestFromJson?.isMandatory == true),
            newFeatures = resolvedFeatures,
            performanceImprovements = parsedPerformance,
            bugFixes = parsedBugFixes,
            versionHistory = manifestFromJson?.versionHistory.orEmpty()
        )

        val historyWithRemote = buildList {
            if (manifest.latestVersion.isNotBlank() || manifest.latestVersionCode > 0) {
                add(
                    VersionRelease(
                        version = manifest.latestVersion.ifBlank { "Build ${manifest.latestVersionCode}" },
                        versionCode = manifest.latestVersionCode,
                        releaseDate = manifest.releaseDate,
                        summary = manifest.summary.ifBlank {
                            "Published via $SOURCE_LABEL_REMOTE_CONFIG."
                        },
                        newFeatures = manifest.newFeatures,
                        performanceImprovements = manifest.performanceImprovements,
                        bugFixes = manifest.bugFixes,
                        isInstalledBuild = false
                    )
                )
            }
            addAll(manifest.versionHistory)
        }

        return if (updateRepository.isUpdateAvailable(manifest, currentVersionCode, currentVersionName)) {
            UpdateCheckState.UpdateAvailable(
                manifest.copy(versionHistory = historyWithRemote)
            )
        } else {
            UpdateCheckState.UpToDate(
                checkedAtMillis = checkedAtMillis,
                serverHistory = manifest.versionHistory,
                sourceLabel = SOURCE_LABEL_REMOTE_CONFIG
            )
        }
    }

    internal fun parseChangelogEntries(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Support JSON arrays such as ["Item 1", "Item 2"]
        if (trimmed.startsWith("[")) {
            val decoded = runCatching {
                json.decodeFromString<List<String>>(trimmed)
            }.getOrNull()
            if (decoded != null) {
                return decoded.map { it.trim() }.filter { it.isNotBlank() }
            }
        }

        // Support newline-separated or pipe-separated changelog entries
        val normalized = if (!trimmed.contains('\n') && trimmed.contains(" | ")) {
            trimmed.replace(" | ", "\n")
        } else {
            trimmed
        }

        return normalized.lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("• ")
                    .removePrefix("· ")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun isUsingLocalDefaultSnapshot(
        snapshot: RemoteConfigUpdateSnapshot,
        currentVersionCode: Int,
        currentVersionName: String
    ): Boolean {
        val noCustomJson = snapshot.manifestJsonRaw.isBlank()
        val noCustomNotes = snapshot.changelogRaw.isBlank() &&
            snapshot.performanceRaw.isBlank() &&
            snapshot.bugFixesRaw.isBlank()
        val matchesCurrentVersion = (snapshot.latestVersionCode <= 0 || snapshot.latestVersionCode == currentVersionCode) &&
            (snapshot.latestVersionName.isBlank() || snapshot.latestVersionName.equals(currentVersionName, ignoreCase = true))
        return noCustomJson && noCustomNotes && matchesCurrentVersion
    }

    companion object {
        const val SOURCE_LABEL_REMOTE_CONFIG = "Firebase Remote Config"
        const val DEFAULT_PERIODIC_INTERVAL_MS = 30L * 60L * 1000L // 30 minutes
        const val MIN_PERIODIC_INTERVAL_MS = 60L * 1000L // 1 minute minimum safety guard
        const val DEFAULT_RESUME_STALE_INTERVAL_MS = 5L * 60L * 1000L // 5 minutes
        const val DEFAULT_DOWNLOAD_URL = "${UserPreferences.DEFAULT_UPDATE_MANIFEST_URL}/releases/latest"

        // Firebase Remote Config parameter keys
        const val KEY_LATEST_VERSION_NAME = "app_update_latest_version_name"
        const val KEY_LATEST_VERSION_CODE = "app_update_latest_version_code"
        const val KEY_DOWNLOAD_URL = "app_update_download_url"
        const val KEY_RELEASE_DATE = "app_update_release_date"
        const val KEY_SUMMARY = "app_update_summary"
        const val KEY_CHANGELOG = "app_update_changelog"
        const val KEY_PERFORMANCE_IMPROVEMENTS = "app_update_performance_improvements"
        const val KEY_BUG_FIXES = "app_update_bug_fixes"
        const val KEY_IS_MANDATORY = "app_update_is_mandatory"
        const val KEY_MANIFEST_JSON = "app_update_manifest_json"

        val ALL_REMOTE_CONFIG_KEYS: Set<String> = setOf(
            KEY_LATEST_VERSION_NAME,
            KEY_LATEST_VERSION_CODE,
            KEY_DOWNLOAD_URL,
            KEY_RELEASE_DATE,
            KEY_SUMMARY,
            KEY_CHANGELOG,
            KEY_PERFORMANCE_IMPROVEMENTS,
            KEY_BUG_FIXES,
            KEY_IS_MANDATORY,
            KEY_MANIFEST_JSON
        )

        @Volatile
        var testRemoteConfigSourceOverride: RemoteConfigUpdateSource? = null

        @Volatile
        var testDownloadLauncher: ((Context, String) -> Boolean)? = null

        fun defaultRemoteConfigValues(): Map<String, Any> = mapOf(
            KEY_LATEST_VERSION_NAME to BuildConfig.VERSION_NAME,
            KEY_LATEST_VERSION_CODE to BuildConfig.VERSION_CODE.toLong(),
            KEY_DOWNLOAD_URL to DEFAULT_DOWNLOAD_URL,
            KEY_RELEASE_DATE to "November 2026",
            KEY_SUMMARY to "",
            KEY_CHANGELOG to "",
            KEY_PERFORMANCE_IMPROVEMENTS to "",
            KEY_BUG_FIXES to "",
            KEY_IS_MANDATORY to false,
            KEY_MANIFEST_JSON to ""
        )

        /**
         * Strictly validates that an update download URL uses HTTP(S) and has a valid host.
         */
        fun isValidDownloadUrl(rawUrl: String): Boolean {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return false
            val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.trim().orEmpty()
            return (scheme == "https" || scheme == "http") && host.isNotBlank()
        }

        /**
         * Triggers the update download flow safely via an external ACTION_VIEW intent.
         * Never performs dynamic code loading.
         */
        fun triggerUpdateDownload(context: Context, manifest: UpdateManifest): Boolean {
            val candidateUrl = manifest.downloadUrl.trim().ifBlank { DEFAULT_DOWNLOAD_URL }
            if (!isValidDownloadUrl(candidateUrl)) return false

            testDownloadLauncher?.let { launcher ->
                return launcher(context, candidateUrl)
            }

            return runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(candidateUrl)).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            val exception = task.exception ?: IllegalStateException("Firebase task failed.")
            continuation.resumeWithException(exception)
        }
    }
}
