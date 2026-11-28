package com.example

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ai.AssistantChatHistoryStore
import com.example.auth.FazaAiAccountRepository
import com.example.auth.FazaAiCloudStore
import com.example.auth.FazaAuthGateway
import com.example.auth.FirebaseGoogleAuthGateway
import com.example.data.local.FazaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.UpdateRepository
import com.example.data.repository.VideoLibraryRepository
import com.example.player.PlayerManager
import com.example.update.AppUpdateManager

import android.app.Activity
import android.content.Context
import com.example.ai.AssistantChatMessage
import com.example.auth.FazaGoogleAccount
import com.example.auth.AuthListenerRegistration
import com.example.auth.GlobalAuthStateListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.concurrent.CopyOnWriteArrayList

private const val APP_TAG = "FazaApplication"

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
    val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(
            context = this,
            updateRepository = updateRepository
        )
    }
    val assistantHistoryStore: AssistantChatHistoryStore by lazy { AssistantChatHistoryStore(this) }
    val authGateway: FazaAuthGateway by lazy { FirebaseGoogleAuthGateway() }
    val aiCloudStore: FazaAiCloudStore? by lazy {
        runCatching { FazaAiAccountRepository(this) }
            .onFailure { err ->
                Log.w(APP_TAG, "Firestore AI store unavailable in this environment: ${err.message}")
            }
            .getOrNull()
    }
    val playerManager: PlayerManager by lazy { PlayerManager(this) }

    private val authGatewayOverrideFlow = MutableStateFlow<FazaAuthGateway?>(null)
    private val activeGlobalListenerBindings = CopyOnWriteArrayList<RebindableAuthListener>()

    private inner class RebindableAuthListener(
        val listener: GlobalAuthStateListener
    ) : AuthListenerRegistration {
        @Volatile
        private var currentRegistration: AuthListenerRegistration? = null

        @Volatile
        private var removed: Boolean = false

        @Synchronized
        fun bindTo(gateway: FazaAuthGateway) {
            currentRegistration?.remove()
            currentRegistration = if (removed) null else gateway.addGlobalAuthStateListener(listener)
        }

        @Synchronized
        override fun remove() {
            removed = true
            currentRegistration?.remove()
            currentRegistration = null
            activeGlobalListenerBindings.remove(this)
        }
    }

    @Volatile
    private var aiCloudStoreOverride: FazaAiCloudStore? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val delegatingAuthGateway: FazaAuthGateway by lazy {
        object : FazaAuthGateway {
            override val currentAccountFlow: Flow<FazaGoogleAccount?> =
                authGatewayOverrideFlow.flatMapLatest { override ->
                    (override ?: authGateway).currentAccountFlow
                }

            override fun getCurrentAccount(): FazaGoogleAccount? =
                (authGatewayOverrideFlow.value ?: authGateway).getCurrentAccount()

            override fun addGlobalAuthStateListener(
                listener: GlobalAuthStateListener
            ): AuthListenerRegistration {
                val binding = RebindableAuthListener(listener)
                activeGlobalListenerBindings.add(binding)
                binding.bindTo(authGatewayOverrideFlow.value ?: authGateway)
                return binding
            }

            override suspend fun attemptSilentSignIn(context: Context): Result<FazaGoogleAccount?> =
                (authGatewayOverrideFlow.value ?: authGateway).attemptSilentSignIn(context)

            override suspend fun signInWithGoogle(activity: Activity): Result<FazaGoogleAccount> =
                (authGatewayOverrideFlow.value ?: authGateway).signInWithGoogle(activity)

            override suspend fun signOut(context: Context): Result<Unit> =
                (authGatewayOverrideFlow.value ?: authGateway).signOut(context)

            override suspend fun verifyActiveSession(): Result<FazaGoogleAccount?> =
                (authGatewayOverrideFlow.value ?: authGateway).verifyActiveSession()
        }
    }

    private val delegatingAiCloudStore: FazaAiCloudStore by lazy {
        object : FazaAiCloudStore {
            override suspend fun upsertUserAccount(account: FazaGoogleAccount): Result<Unit> =
                (aiCloudStoreOverride ?: aiCloudStore)?.upsertUserAccount(account) ?: Result.success(Unit)

            override fun observeUserMessages(userId: String): Flow<List<AssistantChatMessage>> =
                (aiCloudStoreOverride ?: aiCloudStore)?.observeUserMessages(userId) ?: emptyFlow()

            override suspend fun saveMessage(
                account: FazaGoogleAccount,
                message: AssistantChatMessage
            ): Result<Unit> =
                (aiCloudStoreOverride ?: aiCloudStore)?.saveMessage(account, message) ?: Result.success(Unit)

            override suspend fun clearMessages(userId: String): Result<Unit> =
                (aiCloudStoreOverride ?: aiCloudStore)?.clearMessages(userId) ?: Result.success(Unit)
        }
    }

    @VisibleForTesting
    fun setTestAuthDependencies(
        gateway: FazaAuthGateway?,
        cloudStore: FazaAiCloudStore?
    ) {
        aiCloudStoreOverride = cloudStore
        authGatewayOverrideFlow.value = gateway
        val targetGateway = gateway ?: authGateway
        activeGlobalListenerBindings.forEach { it.bindTo(targetGateway) }
    }

    fun activeAuthGateway(): FazaAuthGateway = delegatingAuthGateway

    fun activeAiCloudStore(): FazaAiCloudStore = delegatingAiCloudStore

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
