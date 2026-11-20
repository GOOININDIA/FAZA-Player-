package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LinkedFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM linked_folders ORDER BY addedAt DESC")
    fun observeLinkedFolders(): Flow<List<LinkedFolder>>

    @Query("SELECT * FROM linked_folders ORDER BY addedAt DESC")
    suspend fun getAllFolders(): List<LinkedFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: LinkedFolder)

    @Query("DELETE FROM linked_folders WHERE treeUri = :treeUri")
    suspend fun deleteFolder(treeUri: String)
}
