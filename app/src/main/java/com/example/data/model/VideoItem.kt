package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [Index(value = ["uri"], unique = true)]
)
data class VideoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val folderName: String,
    val folderUri: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/mp4",
    val formatTag: String = "MP4",
    val codecInfo: String = "H.264",
    val thumbnailPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val lastPositionMs: Long = 0L,
    val isDownloaded: Boolean = true,
    val isPrivate: Boolean = false,
    val isFavourite: Boolean = false,
    val subtitleUri: String? = null
) {
    val resolutionTier: ResolutionTier
        get() {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)
            return when {
                maxDim >= 7000 || minDim >= 4000 -> ResolutionTier.UHD_8K
                maxDim >= 3600 || minDim >= 2000 -> ResolutionTier.UHD_4K
                maxDim >= 1800 || minDim >= 1000 -> ResolutionTier.FHD_1080P
                maxDim >= 1200 || minDim >= 700 -> ResolutionTier.HD_720P
                width > 0 && height > 0 -> ResolutionTier.SD
                else -> ResolutionTier.AUTO
            }
        }

    val progressFraction: Float
        get() = if (durationMs > 0L && lastPositionMs > 0L) {
            (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

enum class ResolutionTier(val badge: String, val rank: Int) {
    UHD_8K("8K UHD", 5),
    UHD_4K("4K UHD", 4),
    FHD_1080P("1080p", 3),
    HD_720P("720p", 2),
    SD("SD", 1),
    AUTO("HD", 0)
}
