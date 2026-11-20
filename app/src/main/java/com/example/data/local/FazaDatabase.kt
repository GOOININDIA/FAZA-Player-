package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LinkedFolder
import com.example.data.model.VideoItem

@Database(
    entities = [VideoItem::class, LinkedFolder::class],
    version = 1,
    exportSchema = false
)
abstract class FazaDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun build(context: Context): FazaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FazaDatabase::class.java,
                "faza_player.db"
            ).build()
        }
    }
}
