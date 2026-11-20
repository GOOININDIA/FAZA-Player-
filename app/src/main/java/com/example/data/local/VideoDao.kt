package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun observeAllVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 20")
    fun observeRecentVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VideoItem?

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoItem?

    @Query("SELECT uri FROM videos")
    suspend fun getAllVideoUris(): List<String>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    suspend fun getAllVideosSnapshot(): List<VideoItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideo(video: VideoItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideos(videos: List<VideoItem>): List<Long>

    @Update
    suspend fun updateVideo(video: VideoItem)

    @Query("UPDATE videos SET lastPlayedAt = :playedAt, lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updatePlaybackProgress(id: Long, playedAt: Long, positionMs: Long)

    @Query("UPDATE videos SET isFavourite = :isFavourite WHERE id = :id")
    suspend fun setFavourite(id: Long, isFavourite: Boolean)

    @Query("UPDATE videos SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivate(id: Long, isPrivate: Boolean)

    @Query("UPDATE videos SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun setDownloaded(id: Long, isDownloaded: Boolean)

    @Query("UPDATE videos SET subtitleUri = :subtitleUri WHERE id = :id")
    suspend fun setSubtitleUri(id: Long, subtitleUri: String?)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<Long>)

    @Query("DELETE FROM videos WHERE folderUri = :folderUri")
    suspend fun deleteVideosByFolderUri(folderUri: String)
}
