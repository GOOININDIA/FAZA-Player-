package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ai.AssistantChatHistoryStore
import com.example.data.local.FazaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.UpdateRepository
import com.example.data.repository.VideoLibraryRepository
import com.example.player.PlayerManager

class FazaApplication : Application(), ImageLoaderFactory {
    val database: FazaDatabase by lazy { FazaDatabase.build(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val videoRepository: VideoLibraryRepository by lazy {
        VideoLibraryRepository(
            context = this,
            videoDao = database.videoDao(),
            folderDao = database.folderDao()
        )
    }
    val updateRepository: UpdateRepository by lazy { UpdateRepository() }
    val assistantHistoryStore: AssistantChatHistoryStore by lazy { AssistantChatHistoryStore(this) }
    val playerManager: PlayerManager by lazy { PlayerManager(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("faza_image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(150)
            .build()
    }

    override fun onTerminate() {
        playerManager.release()
        super.onTerminate()
    }
}
