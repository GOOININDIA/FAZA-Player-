package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.FolderSummary
import com.example.data.model.LibrarySection
import com.example.data.model.SortOption
import com.example.data.model.StreamProtocol
import com.example.data.model.VideoItem
import com.example.data.repository.VideoLibraryRepository
import com.example.util.VideoDeduplicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val videos: List<VideoItem> = emptyList(),
    val allPublicVideos: List<VideoItem> = emptyList(),
    val recentVideos: List<VideoItem> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val activeSection: LibrarySection = LibrarySection.ALL,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val isImporting: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasCompletedInitialScan: Boolean = false,
    val statusMessage: String? = null,
    val isPrivateUnlocked: Boolean = false,
    val hasPrivatePin: Boolean = false,
    val linkedFolderCount: Int = 0,
    val networkStreams: List<VideoItem> = emptyList()
)

class LibraryViewModel(
    private val videoRepository: VideoLibraryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val activeSection = MutableStateFlow(LibrarySection.ALL)
    private val searchQuery = MutableStateFlow("")
    private val sortOptionOverride = MutableStateFlow<SortOption?>(null)
    private val isImporting = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val hasCompletedInitialScan = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val isPrivateUnlocked = MutableStateFlow(false)
    private var lastAutoDetectMillis = 0L
    private var lastLoggedDedupCounts: Pair<Int, Int>? = null

    init {
        runInitialAutoDetect()
    }

    private fun runInitialAutoDetect() {
        lastAutoDetectMillis = System.currentTimeMillis()
        viewModelScope.launch {
            isImporting.value = true
            try {
                val result = videoRepository.autoDetectDeviceVideos()
                if (result.addedCount > 0) {
                    statusMessage.value = "Detected ${result.addedCount} new video${if (result.addedCount == 1) "" else "s"}"
                }
            } finally {
                isImporting.value = false
                hasCompletedInitialScan.value = true
            }
        }
    }

    // Observe only the library-relevant preferences so theme/HUD edits never trigger full library recomputation
    private val libraryPrefsFlow = preferencesRepository.preferencesFlow
        .map { prefs -> prefs.privatePin to prefs.videoSortOption }
        .distinctUntilChanged()

    val uiState: StateFlow<LibraryUiState> = combine(
        videoRepository.allVideos.distinctUntilChanged(),
        videoRepository.recentVideos.distinctUntilChanged(),
        videoRepository.linkedFolders.distinctUntilChanged(),
        libraryPrefsFlow,
        combine(activeSection, searchQuery, sortOptionOverride, isImporting, statusMessage) { sec, q, sort, imp, msg ->
            FilterParams(sec, q, sort, imp, msg)
        },
        combine(isPrivateUnlocked, isRefreshing, hasCompletedInitialScan) { unlocked, refreshing, initialDone ->
            Triple(unlocked, refreshing, initialDone)
        }
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val rawAllVideos = flows[0] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val rawRecent = flows[1] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val linkedFolders = flows[2] as List<com.example.data.model.LinkedFolder>
        @Suppress("UNCHECKED_CAST")
        val libraryPrefs = flows[3] as Pair<String?, SortOption>
        val filter = flows[4] as FilterParams
        @Suppress("UNCHECKED_CAST")
        val extra = flows[5] as Triple<Boolean, Boolean, Boolean>
        val unlocked = extra.first
        val refreshing = extra.second
        val initialScanDone = extra.third

        val allVideos = VideoDeduplicator.deduplicate(rawAllVideos)
        val recent = VideoDeduplicator.deduplicate(rawRecent)
        val countSnapshot = rawAllVideos.size to allVideos.size
        if (lastLoggedDedupCounts != countSnapshot) {
            lastLoggedDedupCounts = countSnapshot
            Log.d(
                TAG,
                "FazaMediaList: rawCount=${rawAllVideos.size}, deduplicatedCount=${allVideos.size}, " +
                    "duplicatesFiltered=${rawAllVideos.size - allVideos.size}"
            )
        }

        val hasPin = !libraryPrefs.first.isNullOrBlank()
        val effectiveUnlocked = !hasPin || unlocked
        val effectiveSort = filter.sort ?: libraryPrefs.second

        val publicVideos = allVideos.filter { !it.isPrivate }
        val sectionFiltered = when (filter.section) {
            LibrarySection.ALL -> publicVideos
            LibrarySection.RECENT -> recent.filter { !it.isPrivate }
            LibrarySection.DOWNLOADED -> publicVideos.filter { it.isDownloaded }
            LibrarySection.FAVOURITES -> publicVideos.filter { it.isFavourite }
            LibrarySection.PRIVATE -> if (effectiveUnlocked) allVideos.filter { it.isPrivate } else emptyList()
        }

        val queryFiltered = filterVideosByQuery(sectionFiltered, filter.query)
        val sorted = sortVideos(queryFiltered, effectiveSort)

        val linkedTreeSet = linkedFolders.mapTo(HashSet(linkedFolders.size)) { it.treeUri }
        val groupedFolders = publicVideos
            .filterNot { it.folderName == NETWORK_STREAMS_FOLDER && it.folderUri == null }
            .groupBy { it.folderUri ?: it.folderName }
            .map { (key, list) ->
                val sample = list.first()
                val previewThumb = list.firstOrNull { !it.thumbnailPath.isNullOrBlank() }?.thumbnailPath
                FolderSummary(
                    name = sample.folderName,
                    folderUri = sample.folderUri,
                    videoCount = list.size,
                    totalSizeBytes = list.sumOf { it.sizeBytes },
                    totalDurationMs = list.sumOf { it.durationMs },
                    isSafLinked = key in linkedTreeSet,
                    thumbnailPath = previewThumb
                )
            }
            .toMutableList()

        // Ensure linked SAF folders with 0 discovered videos still appear
        val existingFolderUris = groupedFolders.mapNotNullTo(HashSet()) { it.folderUri }
        for (linked in linkedFolders) {
            if (linked.treeUri !in existingFolderUris) {
                groupedFolders.add(
                    FolderSummary(
                        name = linked.displayName,
                        folderUri = linked.treeUri,
                        videoCount = 0,
                        totalSizeBytes = 0L,
                        totalDurationMs = 0L,
                        isSafLinked = true,
                        thumbnailPath = null
                    )
                )
            }
        }

        val savedStreams = publicVideos
            .filter { it.uri.startsWith("http://", ignoreCase = true) || it.uri.startsWith("https://", ignoreCase = true) }
            .sortedByDescending { it.lastPlayedAt ?: it.dateAdded }

        LibraryUiState(
            videos = sorted,
            allPublicVideos = publicVideos,
            recentVideos = recent.filter { !it.isPrivate },
            folders = groupedFolders.sortedWith(
                compareByDescending<FolderSummary> { it.videoCount }
                    .thenBy { it.name.lowercase() }
            ),
            activeSection = filter.section,
            searchQuery = filter.query,
            sortOption = effectiveSort,
            isImporting = filter.isImporting,
            isRefreshing = refreshing,
            hasCompletedInitialScan = initialScanDone,
            statusMessage = filter.statusMessage,
            isPrivateUnlocked = effectiveUnlocked,
            hasPrivatePin = hasPin,
            linkedFolderCount = linkedFolders.size,
            networkStreams = savedStreams
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState()
        )

    private data class FilterParams(
        val section: LibrarySection,
        val query: String,
        val sort: SortOption?,
        val isImporting: Boolean,
        val statusMessage: String?
    )

    fun setSection(section: LibrarySection) {
        activeSection.value = section
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        sortOptionOverride.value = option
        viewModelScope.launch {
            preferencesRepository.setVideoSortOption(option)
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun unlockPrivateVault(enteredPin: String, expectedPin: String?): Boolean {
        return if (expectedPin.isNullOrBlank() || enteredPin == expectedPin) {
            isPrivateUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockPrivateVault() {
        isPrivateUnlocked.value = false
    }

    fun importVideoUris(uris: List<Uri>, markPrivate: Boolean = false) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting.value = true
            try {
                val count = videoRepository.importUris(
                    uris = uris,
                    folderNameOverride = if (markPrivate) "Private Vault" else "Imported Videos",
                    markPrivate = markPrivate
                )
                statusMessage.value = if (count > 0) {
                    "Added $count video${if (count == 1) "" else "s"} to FAZA Player"
                } else {
                    "Selected video already in your library"
                }
            } finally {
                isImporting.value = false
            }
        }
    }

    fun linkFolder(treeUri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            try {
                val count = videoRepository.linkAndScanFolder(treeUri)
                statusMessage.value = "Folder linked • $count new video${if (count == 1) "" else "s"} detected"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun refreshDeviceStorage() {
        if (isRefreshing.value || isImporting.value) return
        lastAutoDetectMillis = System.currentTimeMillis()
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val result = videoRepository.autoDetectDeviceVideos()
                statusMessage.value = if (result.addedCount > 0) {
                    "Found ${result.addedCount} new video${if (result.addedCount == 1) "" else "s"}"
                } else if (result.linkedFolderCount > 0) {
                    "Scan complete • All ${result.linkedFolderCount} folder${if (result.linkedFolderCount == 1) "" else "s"} up to date"
                } else {
                    "Scan complete • Link a folder to scan more directories"
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun autoDetectDeviceVideos(showStatusOnEmpty: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!showStatusOnEmpty && (isImporting.value || isRefreshing.value || now - lastAutoDetectMillis < PASSIVE_SCAN_COOLDOWN_MS)) {
            return
        }
        lastAutoDetectMillis = now
        viewModelScope.launch {
            isImporting.value = true
            try {
                val result = videoRepository.autoDetectDeviceVideos()
                if (result.addedCount > 0) {
                    statusMessage.value = "Detected ${result.addedCount} new video${if (result.addedCount == 1) "" else "s"}"
                } else if (showStatusOnEmpty) {
                    statusMessage.value = if (result.linkedFolderCount > 0) {
                        "All linked folders are up to date"
                    } else {
                        "Choose a device folder to enable auto-detection"
                    }
                }
            } finally {
                isImporting.value = false
            }
        }
    }

    fun removeFolder(folderUri: String) {
        viewModelScope.launch {
            videoRepository.removeLinkedFolder(folderUri)
            statusMessage.value = "Unlinked folder from library"
        }
    }

    fun addNetworkStream(
        url: String,
        title: String?,
        protocol: StreamProtocol = StreamProtocol.AUTO,
        onReady: (Long) -> Unit
    ) {
        viewModelScope.launch {
            isImporting.value = true
            try {
                val item = videoRepository.addNetworkStream(url, title, protocol)
                if (item != null) {
                    onReady(item.id)
                } else {
                    statusMessage.value = "Please enter a valid http:// or https:// video stream URL"
                }
            } finally {
                isImporting.value = false
            }
        }
    }

    fun openExternalUri(uri: Uri, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val item = videoRepository.ensureExternalUriPlayable(uri)
            if (item != null) {
                onReady(item.id)
            } else {
                statusMessage.value = "Unable to open external video"
            }
        }
    }

    fun toggleFavourite(video: VideoItem) {
        viewModelScope.launch {
            videoRepository.toggleFavourite(video)
        }
    }

    fun togglePrivate(video: VideoItem) {
        viewModelScope.launch {
            videoRepository.togglePrivate(video)
            statusMessage.value = if (!video.isPrivate) "Moved to Private Vault" else "Removed from Private Vault"
        }
    }

    fun removeVideo(videoId: Long) {
        viewModelScope.launch {
            videoRepository.removeVideo(videoId)
            statusMessage.value = "Removed from library"
        }
    }

    companion object {
        private const val TAG = "LibraryViewModel"
        private const val NETWORK_STREAMS_FOLDER = "Network Streams"
        private const val PASSIVE_SCAN_COOLDOWN_MS = 15_000L

        fun filterVideosByQuery(videos: List<VideoItem>, query: String): List<VideoItem> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return videos
            return videos.filter { video ->
                video.title.lowercase().contains(q) ||
                    video.folderName.lowercase().contains(q) ||
                    video.formatTag.lowercase().contains(q) ||
                    video.resolutionTier.badge.lowercase().contains(q)
            }
        }

        fun sortVideos(videos: List<VideoItem>, option: SortOption): List<VideoItem> {
            return when (option) {
                SortOption.DATE_ADDED_DESC -> videos.sortedWith(
                    compareByDescending<VideoItem> { it.dateAdded }.thenBy { it.title.lowercase() }
                )
                SortOption.NAME_ASC -> videos.sortedWith(
                    compareBy<VideoItem> { it.title.lowercase() }.thenByDescending { it.dateAdded }
                )
                SortOption.NAME_DESC -> videos.sortedWith(
                    compareByDescending<VideoItem> { it.title.lowercase() }.thenByDescending { it.dateAdded }
                )
                SortOption.DURATION_DESC -> videos.sortedWith(
                    compareByDescending<VideoItem> { it.durationMs }.thenBy { it.title.lowercase() }
                )
                SortOption.SIZE_DESC -> videos.sortedWith(
                    compareByDescending<VideoItem> { it.sizeBytes }.thenBy { it.title.lowercase() }
                )
                SortOption.RESOLUTION_DESC -> videos.sortedWith(
                    compareByDescending<VideoItem> { it.resolutionTier.rank }
                        .thenByDescending { maxOf(it.width, it.height) }
                        .thenBy { it.title.lowercase() }
                )
            }
        }

        fun factory(
            videoRepository: VideoLibraryRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(videoRepository, preferencesRepository) as T
            }
        }
    }
}
