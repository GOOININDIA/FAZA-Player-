package com.example.util

import com.example.data.model.VideoItem

data class DeduplicationOutcome(
    val uniqueVideos: List<VideoItem>,
    val updatedWinners: List<VideoItem>,
    val duplicateIdsToDelete: List<Long>,
    val rawCount: Int
) {
    val deduplicatedCount: Int get() = uniqueVideos.size
    val duplicatesRemovedCount: Int get() = (rawCount - deduplicatedCount).coerceAtLeast(0)
}

/**
 * Canonical video deduplication engine shared across scanner ingestion, Room cleanup,
 * and UI state projection.
 *
 * Matches duplicates via:
 * 1. Canonical URI / normalized file path / SAF document path / MediaStore ID.
 * 2. Fallback content signature: normalized file name + file size + rounded duration
 *    (plus same-folder size/duration fallbacks when one scanner omits size or duration).
 */
object VideoDeduplicator {

    fun buildKeys(
        uri: String,
        title: String,
        folderName: String,
        sizeBytes: Long,
        durationMs: Long,
        knownFilePath: String? = null
    ): Set<String> {
        val keys = LinkedHashSet<String>(5)

        val canonicalUriKey = normalizeUriKey(uri)
        if (canonicalUriKey.isNotEmpty()) {
            keys.add(canonicalUriKey)
        }

        if (!knownFilePath.isNullOrBlank()) {
            val normalizedPath = normalizeFilePath(knownFilePath)
            if (normalizedPath.isNotEmpty()) {
                keys.add("path:$normalizedPath")
            }
        }

        val isNetworkStream = uri.startsWith("http://", ignoreCase = true) ||
            uri.startsWith("https://", ignoreCase = true) ||
            uri.startsWith("rtsp://", ignoreCase = true) ||
            uri.startsWith("rtmp://", ignoreCase = true)

        if (!isNetworkStream) {
            val normTitle = normalizeTitle(title)
            val normFolder = normalizeFolder(folderName)
            val roundedDurationSec = if (durationMs > 0L) (durationMs + 500L) / 1000L else 0L

            if (normTitle.isNotEmpty()) {
                if (sizeBytes > 0L && roundedDurationSec > 0L) {
                    keys.add("sig:$normTitle|sz:$sizeBytes|dur:$roundedDurationSec")
                }
                if (sizeBytes > 0L && normFolder.isNotEmpty()) {
                    keys.add("sig_sz:$normTitle|sz:$sizeBytes|folder:$normFolder")
                }
                if (roundedDurationSec > 0L && normFolder.isNotEmpty()) {
                    keys.add("sig_dur:$normTitle|dur:$roundedDurationSec|folder:$normFolder")
                }
            }
        }

        return keys
    }

    fun buildKeys(item: VideoItem, knownFilePath: String? = null): Set<String> = buildKeys(
        uri = item.uri,
        title = item.title,
        folderName = item.folderName,
        sizeBytes = item.sizeBytes,
        durationMs = item.durationMs,
        knownFilePath = knownFilePath
    )

    fun deduplicate(videos: List<VideoItem>): List<VideoItem> {
        if (videos.size <= 1) return videos
        return deduplicateWithRemovals(videos).uniqueVideos
    }

    fun deduplicateWithRemovals(videos: List<VideoItem>): DeduplicationOutcome {
        if (videos.isEmpty()) {
            return DeduplicationOutcome(
                uniqueVideos = emptyList(),
                updatedWinners = emptyList(),
                duplicateIdsToDelete = emptyList(),
                rawCount = 0
            )
        }

        val keyToGroupIndex = HashMap<String, Int>(videos.size * 3)
        val groupWinners = mutableListOf<VideoItem>()
        val groupOriginalWinners = mutableListOf<VideoItem>()
        val groupKeys = mutableListOf<MutableSet<String>>()
        val removedIds = mutableListOf<Long>()

        for (candidate in videos) {
            val candidateKeys = buildKeys(candidate)
            var matchedGroupIdx = -1
            for (k in candidateKeys) {
                val existingIdx = keyToGroupIndex[k]
                if (existingIdx != null) {
                    matchedGroupIdx = existingIdx
                    break
                }
            }

            if (matchedGroupIdx == -1) {
                val newIdx = groupWinners.size
                groupWinners.add(candidate)
                groupOriginalWinners.add(candidate)
                val trackedKeys = candidateKeys.toMutableSet()
                groupKeys.add(trackedKeys)
                for (k in trackedKeys) {
                    keyToGroupIndex[k] = newIdx
                }
            } else {
                val currentWinner = groupWinners[matchedGroupIdx]
                val (preferred, discarded) = choosePreferred(currentWinner, candidate)
                val merged = mergeDuplicateMetadata(preferred, discarded)

                if (discarded.id > 0L && discarded.id != merged.id) {
                    removedIds.add(discarded.id)
                }

                // If candidate replaced currentWinner, update the original baseline for DB update comparison
                if (preferred.id == candidate.id && currentWinner.id != candidate.id) {
                    groupOriginalWinners[matchedGroupIdx] = candidate
                }
                groupWinners[matchedGroupIdx] = merged

                val trackedKeys = groupKeys[matchedGroupIdx]
                val mergedKeys = buildKeys(merged)
                trackedKeys.addAll(candidateKeys)
                trackedKeys.addAll(mergedKeys)
                for (k in trackedKeys) {
                    keyToGroupIndex[k] = matchedGroupIdx
                }
            }
        }

        val updatedWinners = buildList {
            for (i in groupWinners.indices) {
                val finalItem = groupWinners[i]
                val originalItem = groupOriginalWinners[i]
                if (finalItem.id > 0L && finalItem != originalItem) {
                    add(finalItem)
                }
            }
        }

        return DeduplicationOutcome(
            uniqueVideos = groupWinners,
            updatedWinners = updatedWinners,
            duplicateIdsToDelete = removedIds.distinct(),
            rawCount = videos.size
        )
    }

    internal fun normalizeUriKey(uri: String): String {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return ""

        // 1. Direct file:// URI -> canonical filesystem path
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            val rawPath = percentDecode(trimmed.substring(7).substringBefore('?').substringBefore('#'))
            val normPath = normalizeFilePath(rawPath)
            if (normPath.isNotEmpty()) return "path:$normPath"
        }

        // 2. Raw absolute path
        if (trimmed.startsWith("/")) {
            val normPath = normalizeFilePath(percentDecode(trimmed))
            if (normPath.isNotEmpty()) return "path:$normPath"
        }

        // 3. SAF ExternalStorage document URI -> canonical filesystem path when pointing to primary storage
        if (trimmed.contains("com.android.externalstorage.documents", ignoreCase = true)) {
            val docSegment = trimmed.substringAfter("/document/", missingDelimiterValue = "")
                .substringBefore('?')
                .substringBefore('#')
            if (docSegment.isNotEmpty()) {
                val decodedDoc = percentDecode(docSegment)
                if (decodedDoc.startsWith("primary:", ignoreCase = true)) {
                    val relPath = decodedDoc.substring(8).trimStart('/')
                    val normPath = normalizeFilePath("/storage/emulated/0/$relPath")
                    if (normPath.isNotEmpty()) return "path:$normPath"
                } else if (decodedDoc.contains(':')) {
                    val volume = decodedDoc.substringBefore(':').lowercase()
                    val relPath = decodedDoc.substringAfter(':').trimStart('/')
                    val normPath = normalizeFilePath("/storage/$volume/$relPath")
                    if (normPath.isNotEmpty()) return "path:$normPath"
                }
            }
        }

        // 4. MediaStore content URI -> canonical MediaStore ID key
        if (trimmed.startsWith("content://media/", ignoreCase = true)) {
            val clean = trimmed.substringBefore('?').substringBefore('#').trimEnd('/')
            val lastSeg = clean.substringAfterLast('/')
            if (lastSeg.isNotEmpty() && lastSeg.all { it.isDigit() }) {
                return "mediastore:$lastSeg"
            }
        }

        // 5. Network streams -> preserve case in path/query, lowercase scheme & host
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("rtsp://", ignoreCase = true) ||
            trimmed.startsWith("rtmp://", ignoreCase = true)
        ) {
            return "url:$trimmed"
        }

        return "uri:${percentDecode(trimmed).lowercase()}"
    }

    internal fun normalizeFilePath(rawPath: String): String {
        val cleaned = rawPath.trim()
            .replace('\\', '/')
            .replace(Regex("/+"), "/")
            .trimEnd('/')
        if (cleaned.isEmpty()) return ""
        val lower = cleaned.lowercase()
        return when {
            lower == "/sdcard" -> "/storage/emulated/0"
            lower.startsWith("/sdcard/") -> "/storage/emulated/0/${lower.removePrefix("/sdcard/")}"
            lower.startsWith("/storage/self/primary/") ->
                "/storage/emulated/0/${lower.removePrefix("/storage/self/primary/")}"
            lower.startsWith("/mnt/sdcard/") ->
                "/storage/emulated/0/${lower.removePrefix("/mnt/sdcard/")}"
            else -> lower
        }
    }

    internal fun normalizeTitle(title: String): String {
        return title.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    internal fun normalizeFolder(folderName: String): String {
        val trimmed = folderName.trim().lowercase()
        return when (trimmed) {
            "downloads" -> "download"
            else -> trimmed
        }
    }

    internal fun percentDecode(input: String): String {
        if (!input.contains('%')) return input
        val out = StringBuilder(input.length)
        val bytes = ByteArray(input.length)
        var i = 0
        while (i < input.length) {
            if (input[i] == '%' && i + 2 < input.length) {
                var byteCount = 0
                var j = i
                while (j + 2 < input.length && input[j] == '%') {
                    val hi = Character.digit(input[j + 1], 16)
                    val lo = Character.digit(input[j + 2], 16)
                    if (hi == -1 || lo == -1) break
                    bytes[byteCount++] = ((hi shl 4) + lo).toByte()
                    j += 3
                }
                if (byteCount > 0) {
                    out.append(String(bytes, 0, byteCount, Charsets.UTF_8))
                    i = j
                    continue
                }
            }
            out.append(input[i])
            i++
        }
        return out.toString()
    }

    private fun choosePreferred(a: VideoItem, b: VideoItem): Pair<VideoItem, VideoItem> {
        val scoreA = preferenceScore(a)
        val scoreB = preferenceScore(b)
        return when {
            scoreB > scoreA -> b to a
            scoreA > scoreB -> a to b
            a.id > 0L && b.id > 0L && b.id < a.id -> b to a
            else -> a to b
        }
    }

    private fun preferenceScore(item: VideoItem): Int {
        var score = 0
        if (item.lastPlayedAt != null || item.lastPositionMs > 0L) score += 40
        if (item.isFavourite) score += 20
        if (item.isPrivate) score += 20
        if (!item.subtitleUri.isNullOrBlank()) score += 15
        if (!item.thumbnailPath.isNullOrBlank()) score += 10
        if (item.durationMs > 0L) score += 6
        if (item.sizeBytes > 0L) score += 5
        if (item.width > 0 && item.height > 0) score += 5
        if (item.uri.startsWith("content://media/", ignoreCase = true)) score += 4
        if (item.uri.startsWith("file://", ignoreCase = true)) score += 3
        if (item.folderUri != null) score += 2
        return score
    }

    private fun mergeDuplicateMetadata(winner: VideoItem, other: VideoItem): VideoItem {
        val winnerPlayed = winner.lastPlayedAt ?: 0L
        val otherPlayed = other.lastPlayedAt ?: 0L
        val latestPlayedAt = when {
            winner.lastPlayedAt == null && other.lastPlayedAt == null -> null
            otherPlayed > winnerPlayed -> other.lastPlayedAt
            else -> winner.lastPlayedAt
        }
        val bestPositionMs = when {
            otherPlayed > winnerPlayed && other.lastPositionMs > 0L -> other.lastPositionMs
            winner.lastPositionMs > 0L -> winner.lastPositionMs
            else -> other.lastPositionMs
        }

        return winner.copy(
            durationMs = if (winner.durationMs > 0L) winner.durationMs else other.durationMs,
            sizeBytes = if (winner.sizeBytes > 0L) winner.sizeBytes else other.sizeBytes,
            width = if (winner.width > 0) winner.width else other.width,
            height = if (winner.height > 0) winner.height else other.height,
            thumbnailPath = winner.thumbnailPath?.takeIf { it.isNotBlank() } ?: other.thumbnailPath,
            subtitleUri = winner.subtitleUri?.takeIf { it.isNotBlank() } ?: other.subtitleUri,
            folderUri = winner.folderUri ?: other.folderUri,
            lastPlayedAt = latestPlayedAt,
            lastPositionMs = bestPositionMs,
            isFavourite = winner.isFavourite || other.isFavourite,
            isPrivate = winner.isPrivate || other.isPrivate,
            isDownloaded = winner.isDownloaded || other.isDownloaded
        )
    }
}

/**
 * Mutable lookup index used during a multi-source storage scan so duplicate entries
 * across SAF trees, filesystem walking, and MediaStore queries are rejected immediately
 * before metadata extraction or database insertion.
 */
class VideoDeduplicationIndex(initialVideos: List<VideoItem> = emptyList()) {
    private val knownKeys = HashSet<String>(initialVideos.size * 3 + 32)

    init {
        initialVideos.forEach { record(it) }
    }

    fun contains(
        uri: String,
        title: String,
        folderName: String,
        sizeBytes: Long,
        durationMs: Long,
        knownFilePath: String? = null
    ): Boolean {
        val keys = VideoDeduplicator.buildKeys(
            uri = uri,
            title = title,
            folderName = folderName,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            knownFilePath = knownFilePath
        )
        return keys.any { it in knownKeys }
    }

    fun containsUri(uri: String): Boolean {
        val key = VideoDeduplicator.normalizeUriKey(uri)
        return key.isNotEmpty() && key in knownKeys
    }

    fun contains(item: VideoItem, knownFilePath: String? = null): Boolean {
        val keys = VideoDeduplicator.buildKeys(item, knownFilePath)
        return keys.any { it in knownKeys }
    }

    fun record(item: VideoItem, knownFilePath: String? = null) {
        knownKeys.addAll(VideoDeduplicator.buildKeys(item, knownFilePath))
    }

    fun recordKeys(
        uri: String,
        title: String,
        folderName: String,
        sizeBytes: Long,
        durationMs: Long,
        knownFilePath: String? = null
    ) {
        knownKeys.addAll(
            VideoDeduplicator.buildKeys(
                uri = uri,
                title = title,
                folderName = folderName,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                knownFilePath = knownFilePath
            )
        )
    }
}
