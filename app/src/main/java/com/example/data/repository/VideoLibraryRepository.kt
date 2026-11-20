package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.FolderDao
import com.example.data.local.VideoDao
import com.example.data.model.LinkedFolder
import com.example.data.model.NetworkStreamValidator
import com.example.data.model.StreamProtocol
import com.example.data.model.StreamUrlValidation
import com.example.data.model.VideoItem
import com.example.util.VideoDeduplicationIndex
import com.example.util.VideoDeduplicator
import com.example.util.hasAllFilesAccessPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AutoDetectResult(
    val addedCount: Int,
    val linkedFolderCount: Int,
    val rawScannedCount: Int = 0,
    val deduplicatedCount: Int = 0,
    val duplicatesRemovedCount: Int = 0
)

class VideoLibraryRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val folderDao: FolderDao
) {
    private val scanMutex = Mutex()
    private val supportedExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "flv", "webm", "m4v", "3gp", "ts", "mts", "wmv", "mpeg", "mpg"
    )
    private val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt", "sub")

    val allVideos: Flow<List<VideoItem>> = videoDao.observeAllVideos()
    val recentVideos: Flow<List<VideoItem>> = videoDao.observeRecentVideos()
    val linkedFolders: Flow<List<LinkedFolder>> = folderDao.observeLinkedFolders()

    suspend fun getVideoById(id: Long): VideoItem? = videoDao.getVideoById(id)

    suspend fun importUris(
        uris: List<Uri>,
        folderNameOverride: String = "Imported Videos",
        markPrivate: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val existingVideos = videoDao.getAllVideosSnapshot()
        val dedupIndex = VideoDeduplicationIndex(existingVideos)
        val toInsert = mutableListOf<VideoItem>()
        for (uri in uris) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Photo Picker URIs may not support persistable grants; still playable in-session.
            } catch (_: Exception) {
            }

            val uriStr = uri.toString()
            if (dedupIndex.containsUri(uriStr)) {
                if (markPrivate) {
                    val existing = videoDao.getVideoByUri(uriStr)
                    if (existing != null && !existing.isPrivate) {
                        videoDao.setPrivate(existing.id, true)
                    }
                }
                continue
            }

            val video = extractVideoMetadata(
                uri = uri,
                folderName = folderNameOverride,
                folderUri = null,
                isDownloaded = true,
                isPrivate = markPrivate
            )
            if (video != null && !dedupIndex.contains(video)) {
                toInsert.add(video)
                dedupIndex.record(video)
            }
        }
        val inserted = insertInBatches(toInsert)
        Log.d(
            TAG,
            "FazaMediaImport: rawRequested=${uris.size}, deduplicatedInserted=$inserted"
        )
        inserted
    }

    suspend fun linkAndScanFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val folderName = rootDoc.name ?: "Device Folder"
        val existingVideos = videoDao.getAllVideosSnapshot()
        val dedupIndex = VideoDeduplicationIndex(existingVideos)
        val newlyDiscovered = mutableListOf<VideoItem>()
        val totalFound = scanDocumentTree(
            doc = rootDoc,
            rootFolderName = folderName,
            treeUriString = treeUri.toString(),
            depth = 0,
            dedupIndex = dedupIndex,
            outNew = newlyDiscovered
        )

        val inserted = insertInBatches(newlyDiscovered)

        folderDao.upsertFolder(
            LinkedFolder(
                treeUri = treeUri.toString(),
                displayName = folderName,
                videoCount = totalFound
            )
        )
        Log.d(
            TAG,
            "FazaMediaScan(SAF): folder=$folderName, rawScanned=$totalFound, newlyAdded=$inserted"
        )
        inserted
    }

    suspend fun rescanLinkedFolder(
        folder: LinkedFolder,
        dedupIndex: VideoDeduplicationIndex? = null,
        rawCountOut: IntArray? = null
    ): Int = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folder.treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val index = dedupIndex ?: VideoDeduplicationIndex(videoDao.getAllVideosSnapshot())
        val newlyDiscovered = mutableListOf<VideoItem>()
        val totalFound = scanDocumentTree(
            doc = rootDoc,
            rootFolderName = folder.displayName,
            treeUriString = folder.treeUri,
            depth = 0,
            dedupIndex = index,
            outNew = newlyDiscovered
        )
        if (rawCountOut != null && rawCountOut.isNotEmpty()) {
            rawCountOut[0] += totalFound
        }
        val inserted = insertInBatches(newlyDiscovered)
        if (folder.videoCount != totalFound) {
            folderDao.upsertFolder(folder.copy(videoCount = totalFound))
        }
        inserted
    }

    suspend fun autoDetectDeviceVideos(): AutoDetectResult = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            // 1. Clean up any pre-existing duplicate rows already stored in Room
            val existingBefore = videoDao.getAllVideosSnapshot()
            val cleanup = VideoDeduplicator.deduplicateWithRemovals(existingBefore)
            for (updated in cleanup.updatedWinners) {
                videoDao.updateVideo(updated)
            }
            if (cleanup.duplicateIdsToDelete.isNotEmpty()) {
                for (idChunk in cleanup.duplicateIdsToDelete.chunked(DELETE_BATCH_SIZE)) {
                    videoDao.deleteVideosByIds(idChunk)
                }
            }

            var added = 0
            val rawCounter = IntArray(1)
            val knownFolders = folderDao.getAllFolders().associateBy { it.treeUri }.toMutableMap()
            val dedupIndex = VideoDeduplicationIndex(cleanup.uniqueVideos)

            // Include any persisted SAF folder permissions that might not be recorded yet
            val persistedTrees = try {
                context.contentResolver.persistedUriPermissions
                    .filter { it.isReadPermission }
                    .map { it.uri }
            } catch (_: Exception) {
                emptyList()
            }

            for (treeUri in persistedTrees) {
                val key = treeUri.toString()
                if (key !in knownFolders) {
                    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDoc != null && rootDoc.isDirectory) {
                        val folder = LinkedFolder(
                            treeUri = key,
                            displayName = rootDoc.name ?: "Device Folder",
                            videoCount = 0
                        )
                        folderDao.upsertFolder(folder)
                        knownFolders[key] = folder
                    }
                }
            }

            // Rescan all linked/persisted SAF folders skipping canonical duplicates
            for (folder in knownFolders.values) {
                try {
                    added += rescanLinkedFolder(folder, dedupIndex, rawCounter)
                } catch (_: Exception) {
                }
            }

            // When system-level All Files Access is granted, scan shared device storage for videos & subtitles
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasAllFilesAccessPermission()) {
                added += scanExternalStorageVideos(dedupIndex, rawCounter)
            }

            // Also scan any caller-accessible MediaStore videos without requesting broad storage permissions
            added += scanAccessibleMediaStoreVideos(dedupIndex, rawCounter)

            val finalDeduplicatedCount = cleanup.deduplicatedCount + added
            Log.d(
                TAG,
                "FazaMediaScan: rawScanned=${rawCounter[0]}, existingBefore=${existingBefore.size}, " +
                    "dbDuplicatesRemoved=${cleanup.duplicatesRemovedCount}, newlyAdded=$added, " +
                    "deduplicatedCount=$finalDeduplicatedCount"
            )

            AutoDetectResult(
                addedCount = added,
                linkedFolderCount = knownFolders.size,
                rawScannedCount = rawCounter[0],
                deduplicatedCount = finalDeduplicatedCount,
                duplicatesRemovedCount = cleanup.duplicatesRemovedCount
            )
        }
    }

    private suspend fun scanExternalStorageVideos(
        dedupIndex: VideoDeduplicationIndex,
        rawCountOut: IntArray
    ): Int {
        val root = runCatching { Environment.getExternalStorageDirectory() }.getOrNull() ?: return 0
        if (!root.exists() || !root.isDirectory || !root.canRead()) return 0

        val discovered = mutableListOf<File>()
        collectVideoFiles(dir = root, depth = 0, out = discovered)
        rawCountOut[0] += discovered.size
        val batch = mutableListOf<VideoItem>()
        var added = 0
        for (file in discovered) {
            val canonicalPath = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            val fileUri = Uri.fromFile(file)
            val uriStr = fileUri.toString()
            val folderName = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Device Storage"
            val fileSize = file.length()
            if (dedupIndex.contains(
                    uri = uriStr,
                    title = file.name,
                    folderName = folderName,
                    sizeBytes = fileSize,
                    durationMs = 0L,
                    knownFilePath = canonicalPath
                )
            ) {
                continue
            }

            val ext = file.extension.lowercase()
            val subtitleUri = findMatchingSubtitleFile(file)?.let { Uri.fromFile(it).toString() }
            val extracted = extractVideoMetadata(
                uri = fileUri,
                folderName = folderName,
                folderUri = null,
                isDownloaded = true,
                isPrivate = false,
                knownName = file.name,
                knownSize = fileSize,
                knownMime = "video/$ext"
            )?.copy(subtitleUri = subtitleUri)

            if (extracted != null && !dedupIndex.contains(extracted, canonicalPath)) {
                dedupIndex.record(extracted, canonicalPath)
                batch.add(extracted)
                if (batch.size >= INSERT_BATCH_SIZE) {
                    added += insertInBatches(batch)
                    batch.clear()
                }
            }
        }
        if (batch.isNotEmpty()) {
            added += insertInBatches(batch)
        }
        return added
    }

    private fun collectVideoFiles(dir: File, depth: Int, out: MutableList<File>) {
        if (!shouldScanDirectory(dir.name, depth, isRoot = depth == 0)) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                collectVideoFiles(child, depth + 1, out)
            } else if (child.isFile && child.extension.lowercase() in supportedExtensions) {
                out.add(child)
            }
        }
    }

    private fun findMatchingSubtitleFile(videoFile: File): File? {
        val parent = videoFile.parentFile ?: return null
        val baseName = videoFile.nameWithoutExtension
        for (ext in subtitleExtensions) {
            val candidate = File(parent, "$baseName.$ext")
            if (candidate.exists() && candidate.isFile) return candidate
        }
        return null
    }

    private suspend fun scanAccessibleMediaStoreVideos(
        dedupIndex: VideoDeduplicationIndex,
        rawCountOut: IntArray
    ): Int {
        var added = 0
        val batch = mutableListOf<VideoItem>()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            @Suppress("DEPRECATION")
            add(MediaStore.Video.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            }
        }.toTypedArray()
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                @Suppress("DEPRECATION")
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    if (idCol < 0) continue
                    rawCountOut[0]++
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val uriStr = contentUri.toString()
                    val title = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val safeTitle = title?.takeIf { it.isNotBlank() } ?: "Video_$id.mp4"
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else null) ?: "video/mp4"
                    val bucket = (if (bucketCol >= 0) cursor.getString(bucketCol) else null)
                        ?.takeIf { it.isNotBlank() } ?: "Device Videos"

                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                    val resolvedFilePath = when {
                        !dataPath.isNullOrBlank() -> dataPath
                        !relPath.isNullOrBlank() -> "/storage/emulated/0/${relPath.trimStart('/')}$safeTitle"
                        else -> null
                    }

                    if (dedupIndex.contains(
                            uri = uriStr,
                            title = safeTitle,
                            folderName = bucket,
                            sizeBytes = sizeBytes,
                            durationMs = durationMs,
                            knownFilePath = resolvedFilePath
                        )
                    ) {
                        continue
                    }

                    // Use MediaStore metadata directly whenever available to avoid slow MediaMetadataRetriever opens
                    val extracted = if (durationMs > 0L && width > 0 && height > 0) {
                        val formatTag = resolveFormatTag(safeTitle, mime)
                        val thumbPath = loadOrCreateMediaStoreThumbnail(contentUri, durationMs)
                        VideoItem(
                            uri = uriStr,
                            title = safeTitle,
                            folderName = bucket,
                            folderUri = null,
                            durationMs = durationMs,
                            sizeBytes = sizeBytes,
                            width = width,
                            height = height,
                            mimeType = mime,
                            formatTag = formatTag,
                            codecInfo = resolveCodecHint(formatTag, width, height),
                            thumbnailPath = thumbPath,
                            isDownloaded = true,
                            isPrivate = false
                        )
                    } else {
                        extractVideoMetadata(
                            uri = contentUri,
                            folderName = bucket,
                            folderUri = null,
                            isDownloaded = true,
                            isPrivate = false,
                            knownName = safeTitle,
                            knownSize = sizeBytes,
                            knownMime = mime
                        )?.let { item ->
                            item.copy(
                                durationMs = if (item.durationMs > 0L) item.durationMs else durationMs,
                                width = if (item.width > 0) item.width else width,
                                height = if (item.height > 0) item.height else height
                            )
                        }
                    }

                    if (extracted != null && !dedupIndex.contains(extracted, resolvedFilePath)) {
                        dedupIndex.record(extracted, resolvedFilePath)
                        batch.add(extracted)
                        if (batch.size >= INSERT_BATCH_SIZE) {
                            added += insertInBatches(batch)
                            batch.clear()
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            // Expected on Android 11+ when broad storage permissions are not held.
        } catch (_: Exception) {
        }
        if (batch.isNotEmpty()) {
            added += insertInBatches(batch)
        }
        return added
    }

    private suspend fun insertInBatches(items: List<VideoItem>): Int {
        if (items.isEmpty()) return 0
        var inserted = 0
        for (chunk in items.chunked(INSERT_BATCH_SIZE)) {
            val rowIds = videoDao.insertVideos(chunk)
            inserted += rowIds.count { it > 0L }
        }
        return inserted
    }

    suspend fun removeLinkedFolder(treeUri: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideosByFolderUri(treeUri)
        folderDao.deleteFolder(treeUri)
    }

    suspend fun addNetworkStream(
        url: String,
        customTitle: String? = null,
        protocol: StreamProtocol = StreamProtocol.AUTO
    ): VideoItem? = withContext(Dispatchers.IO) {
        val validation = NetworkStreamValidator.validateUrl(url)
        if (validation !is StreamUrlValidation.Valid) return@withContext null

        val meta = NetworkStreamValidator.resolveStreamMetadata(
            url = validation.normalizedUrl,
            selectedProtocol = protocol,
            customTitle = customTitle
        )

        val existing = videoDao.getVideoByUri(meta.normalizedUrl)
        if (existing != null) {
            val updated = existing.copy(
                title = if (!customTitle.isNullOrBlank()) meta.title else existing.title,
                mimeType = if (protocol != StreamProtocol.AUTO) meta.mimeType else existing.mimeType,
                formatTag = if (protocol != StreamProtocol.AUTO) meta.formatTag else existing.formatTag,
                dateAdded = System.currentTimeMillis()
            )
            if (updated != existing) {
                videoDao.updateVideo(updated)
            }
            return@withContext updated
        }

        val item = VideoItem(
            uri = meta.normalizedUrl,
            title = meta.title,
            folderName = "Network Streams",
            mimeType = meta.mimeType,
            formatTag = meta.formatTag,
            codecInfo = if (meta.formatTag == "HLS" || meta.formatTag == "DASH") "Adaptive Stream" else "Network Stream",
            isDownloaded = false,
            isPrivate = false,
            isFavourite = false
        )
        val insertedId = videoDao.insertVideo(item)
        if (insertedId > 0) item.copy(id = insertedId) else videoDao.getVideoByUri(meta.normalizedUrl)
    }

    suspend fun ensureExternalUriPlayable(uri: Uri): VideoItem? = withContext(Dispatchers.IO) {
        val existing = videoDao.getVideoByUri(uri.toString())
        if (existing != null) return@withContext existing
        val extracted = extractVideoMetadata(
            uri = uri,
            folderName = "External",
            folderUri = null,
            isDownloaded = uri.scheme != "http" && uri.scheme != "https",
            isPrivate = false
        ) ?: return@withContext null
        val id = videoDao.insertVideo(extracted)
        if (id > 0) extracted.copy(id = id) else videoDao.getVideoByUri(uri.toString())
    }

    suspend fun updatePlaybackProgress(videoId: Long, positionMs: Long) {
        videoDao.updatePlaybackProgress(videoId, System.currentTimeMillis(), positionMs)
    }

    suspend fun toggleFavourite(video: VideoItem) {
        videoDao.setFavourite(video.id, !video.isFavourite)
    }

    suspend fun togglePrivate(video: VideoItem) {
        videoDao.setPrivate(video.id, !video.isPrivate)
    }

    suspend fun toggleDownloaded(video: VideoItem) {
        videoDao.setDownloaded(video.id, !video.isDownloaded)
    }

    suspend fun attachSubtitleUri(videoId: Long, subtitleUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                subtitleUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        videoDao.setSubtitleUri(videoId, subtitleUri.toString())
    }

    suspend fun removeVideo(videoId: Long) {
        videoDao.deleteVideoById(videoId)
    }

    private fun scanDocumentTree(
        doc: DocumentFile,
        rootFolderName: String,
        treeUriString: String,
        depth: Int,
        dedupIndex: VideoDeduplicationIndex,
        outNew: MutableList<VideoItem>
    ): Int {
        if (!shouldScanDirectory(doc.name, depth, isRoot = depth == 0)) return 0
        var totalFound = 0
        val children = doc.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                val subName = child.name ?: rootFolderName
                if (shouldScanDirectory(child.name, depth + 1, isRoot = false)) {
                    totalFound += scanDocumentTree(
                        doc = child,
                        rootFolderName = subName,
                        treeUriString = treeUriString,
                        depth = depth + 1,
                        dedupIndex = dedupIndex,
                        outNew = outNew
                    )
                }
            } else if (child.isFile) {
                val name = child.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = child.type ?: ""
                if (mime.startsWith("video/") || ext in supportedExtensions) {
                    totalFound++
                    val uriStr = child.uri.toString()
                    val childSize = child.length()
                    if (dedupIndex.contains(
                            uri = uriStr,
                            title = name,
                            folderName = rootFolderName,
                            sizeBytes = childSize,
                            durationMs = 0L
                        )
                    ) {
                        continue
                    }

                    val extracted = extractVideoMetadata(
                        uri = child.uri,
                        folderName = rootFolderName,
                        folderUri = treeUriString,
                        isDownloaded = true,
                        isPrivate = false,
                        knownName = name,
                        knownSize = childSize,
                        knownMime = mime.ifBlank { "video/$ext" }
                    )
                    if (extracted != null && !dedupIndex.contains(extracted)) {
                        dedupIndex.record(extracted)
                        outNew.add(extracted)
                    }
                }
            }
        }
        return totalFound
    }

    private fun extractVideoMetadata(
        uri: Uri,
        folderName: String,
        folderUri: String?,
        isDownloaded: Boolean,
        isPrivate: Boolean,
        knownName: String? = null,
        knownSize: Long = 0L,
        knownMime: String? = null
    ): VideoItem? {
        var displayName = knownName
        var sizeBytes = knownSize
        val mimeType = knownMime ?: context.contentResolver.getType(uri) ?: "video/mp4"

        if (displayName == null || sizeBytes <= 0L) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (displayName == null && nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx)
                        }
                        if (sizeBytes <= 0L && sizeIdx >= 0) {
                            sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        val finalName = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Video_${System.currentTimeMillis()}.mp4"

        val formatTag = resolveFormatTag(finalName, mimeType)

        var durationMs = 0L
        var width = 0
        var height = 0
        val uriStr = uri.toString()
        var thumbPath: String? = findCachedThumbnailPath(uriStr)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            if (thumbPath == null) {
                val frameTimeUs = if (durationMs > 2000L) 1500_000L else 0L
                val bitmap = extractScaledFrame(retriever, frameTimeUs)
                if (bitmap != null) {
                    thumbPath = saveThumbnail(uriStr, bitmap)
                    bitmap.recycle()
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return VideoItem(
            uri = uriStr,
            title = finalName,
            folderName = folderName,
            folderUri = folderUri,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            mimeType = mimeType,
            formatTag = formatTag,
            codecInfo = resolveCodecHint(formatTag, width, height),
            thumbnailPath = thumbPath,
            isDownloaded = isDownloaded,
            isPrivate = isPrivate
        )
    }

    private fun loadOrCreateMediaStoreThumbnail(contentUri: Uri, durationMs: Long): String? {
        val uriStr = contentUri.toString()
        findCachedThumbnailPath(uriStr)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(
                    contentUri,
                    Size(THUMB_MAX_WIDTH, THUMB_MAX_HEIGHT),
                    null
                )
                val saved = saveThumbnail(uriStr, bitmap)
                bitmap.recycle()
                if (saved != null) return saved
            } catch (_: Exception) {
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, contentUri)
            val frameTimeUs = if (durationMs > 2000L) 1500_000L else 0L
            val bitmap = extractScaledFrame(retriever, frameTimeUs)
            if (bitmap != null) {
                val saved = saveThumbnail(uriStr, bitmap)
                bitmap.recycle()
                saved
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractScaledFrame(retriever: MediaMetadataRetriever, frameTimeUs: Long): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                frameTimeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                THUMB_MAX_WIDTH,
                THUMB_MAX_HEIGHT
            )
        } else {
            retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    }

    private fun thumbnailCacheFile(uriString: String): File {
        val thumbsDir = File(context.cacheDir, "thumbs").apply { mkdirs() }
        return File(thumbsDir, "${uriString.hashCode().toUInt()}.jpg")
    }

    private fun findCachedThumbnailPath(uriString: String): String? {
        val file = thumbnailCacheFile(uriString)
        return if (file.exists() && file.length() > 0L) file.absolutePath else null
    }

    private fun saveThumbnail(uriString: String, bitmap: Bitmap): String? {
        return try {
            val file = thumbnailCacheFile(uriString)
            val targetBitmap = if (bitmap.width > THUMB_MAX_WIDTH) {
                val scaledHeight = ((bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1)) * THUMB_MAX_WIDTH)
                    .toInt().coerceIn(180, THUMB_MAX_WIDTH)
                Bitmap.createScaledBitmap(bitmap, THUMB_MAX_WIDTH, scaledHeight, true)
            } else {
                bitmap
            }
            FileOutputStream(file).use { out ->
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            if (targetBitmap !== bitmap) targetBitmap.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFormatTag(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").uppercase()
        return when {
            ext in setOf("MP4", "MKV", "AVI", "MOV", "FLV", "WEBM", "M4V", "3GP", "TS", "WMV") -> ext
            mimeType.contains("matroska", true) -> "MKV"
            mimeType.contains("webm", true) -> "WEBM"
            mimeType.contains("quicktime", true) -> "MOV"
            mimeType.contains("x-msvideo", true) -> "AVI"
            mimeType.contains("x-flv", true) -> "FLV"
            else -> "MP4"
        }
    }

    private fun resolveCodecHint(formatTag: String, width: Int, height: Int): String = when (formatTag) {
        "MKV", "MP4", "MOV" -> if (maxOf(width, height) >= 3600) "HEVC / H.265" else "H.264 / AVC"
        "WEBM" -> "VP9 / AV1"
        "AVI" -> "MPEG-4 / Xvid"
        "FLV" -> "Sorenson / H.264"
        else -> "H.264"
    }

    companion object {
        private const val TAG = "VideoLibraryRepository"
        internal const val MAX_SCAN_DEPTH = 6
        private const val INSERT_BATCH_SIZE = 25
        private const val DELETE_BATCH_SIZE = 200
        private const val THUMB_MAX_WIDTH = 480
        private const val THUMB_MAX_HEIGHT = 270

        fun shouldScanDirectory(folderName: String?, depth: Int, isRoot: Boolean = false): Boolean {
            if (depth > MAX_SCAN_DEPTH) return false
            if (isRoot) return true
            val trimmed = folderName?.trim().orEmpty()
            if (trimmed.isEmpty()) return true
            if (trimmed.startsWith(".")) return false
            if (trimmed.equals("LOST.DIR", ignoreCase = true)) return false
            if (trimmed.equals("Android", ignoreCase = true)) return false
            return true
        }
    }
}
