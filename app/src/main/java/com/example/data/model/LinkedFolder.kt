package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "linked_folders")
data class LinkedFolder(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val videoCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

data class FolderSummary(
    val name: String,
    val folderUri: String?,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val totalDurationMs: Long,
    val isSafLinked: Boolean,
    val thumbnailPath: String? = null
)
