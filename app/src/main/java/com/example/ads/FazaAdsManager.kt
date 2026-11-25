package com.example.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FazaAdsManager"

interface RewardedAdCallbacks {
    fun onLoadingChanged(isLoading: Boolean)
    fun onRewardEarned(sessionId: String)
    fun onDismissedWithoutReward(sessionId: String)
    fun onFailed(sessionId: String, reason: String)
    fun onOfflineBlocked(sessionId: String)
}

object FazaAdsManager {

    // Official FAZA Player production AdMob credentials (used in release builds)
    const val PRODUCTION_ADMOB_APP_ID = "ca-app-pub-6572024129453689~5808434174"
    const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-6572024129453689/7809859094"
    const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-6572024129453689/8759531574"

    // Official Google AdMob test ad unit IDs (used in debug/development builds per AdMob policy)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val initialized = AtomicBoolean(false)
    private val rewardedRequestInFlight = AtomicBoolean(false)
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Test-only hook allowing JVM/Robolectric tests to deterministically exercise
     * rewarded ad completion, dismissal, duplicate callbacks, and failure paths.
     */
    @Volatile
    internal var testRewardedAdLauncher: ((sessionId: String, callbacks: RewardedAdCallbacks) -> Unit)? = null

    fun isUsingTestAdIds(): Boolean = BuildConfig.USE_TEST_ADS

    fun resolveAppId(): String =
        BuildConfig.ADMOB_APP_ID.trim().ifBlank { PRODUCTION_ADMOB_APP_ID }

    fun resolveBannerAdUnitId(useTestAds: Boolean = isUsingTestAdIds()): String {
        if (useTestAds) return TEST_BANNER_AD_UNIT_ID
        return BuildConfig.ADMOB_BANNER_AD_UNIT_ID
            .trim()
            .takeUnless { it.isBlank() || it == TEST_BANNER_AD_UNIT_ID }
            ?: PRODUCTION_BANNER_AD_UNIT_ID
    }

    fun resolveRewardedAdUnitId(useTestAds: Boolean = isUsingTestAdIds()): String {
        if (useTestAds) return TEST_REWARDED_AD_UNIT_ID
        return BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
            .trim()
            .takeUnless { it.isBlank() || it == TEST_REWARDED_AD_UNIT_ID }
            ?: PRODUCTION_REWARDED_AD_UNIT_ID
    }

    fun isRobolectricRuntime(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true) ||
            Build.MODEL.contains("robolectric", ignoreCase = true)
    }

    internal fun hasValidatedInternetCapability(
        hasInternet: Boolean,
        hasValidated: Boolean,
        isRobolectric: Boolean
    ): Boolean {
        if (!hasInternet) return false
        return if (isRobolectric) true else hasValidated
    }

    fun isInternetConnected(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            hasValidatedInternetCapability(
                hasInternet = hasInternet,
                hasValidated = hasValidated,
                isRobolectric = isRobolectricRuntime()
            )
        }.getOrDefault(false)
    }

    fun observeInternetConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        fun emitCurrent() {
            trySend(isInternetConnected(appContext))
        }

        emitCurrent()

        if (cm == null) {
            awaitClose {}
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitCurrent()
            }

            override fun onLost(network: Network) {
                emitCurrent()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                emitCurrent()
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    /**
     * Initializes the Google Mobile Ads SDK asynchronously ONLY when online.
     * Never performs background initialization while offline or in Robolectric tests.
     */
    fun ensureInitializedAsync(context: Context) {
        if (!isInternetConnected(context)) return
        if (isRobolectricRuntime()) return
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        initScope.launch {
            runCatching {
                MobileAds.initialize(appContext) { status ->
                    Log.d(
                        TAG,
                        "MobileAds initialized (testMode=${isUsingTestAdIds()}, adapters=${status.adapterStatusMap.size})"
                    )
                }
            }.onFailure { error ->
                initialized.set(false)
                Log.w(TAG, "MobileAds initialization skipped: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Loads and immediately shows a Google AdMob Rewarded Ad upon explicit user request.
     * - Uses Google test unit ID in debug builds and official FAZA Player unit ID in release builds.
     * - Never requests ads offline.
     * - Prevents duplicate concurrent ad requests and Activity memory leaks.
     * - Counts reward strictly when Google AdMob invokes onUserEarnedReward (at most once per show).
     */
    fun loadAndShowRewardedAd(
        activity: Activity,
        sessionId: String,
        callbacks: RewardedAdCallbacks
    ) {
        if (!isInternetConnected(activity)) {
            callbacks.onLoadingChanged(false)
            callbacks.onOfflineBlocked(sessionId)
            return
        }

        testRewardedAdLauncher?.let { launcher ->
            launcher(sessionId, callbacks)
            return
        }

        if (isRobolectricRuntime()) {
            callbacks.onLoadingChanged(false)
            callbacks.onFailed(sessionId, "Rewarded ads require an Android device runtime.")
            return
        }

        if (!rewardedRequestInFlight.compareAndSet(false, true)) {
            return
        }

        ensureInitializedAsync(activity)
        callbacks.onLoadingChanged(true)

        val activityRef = WeakReference(activity)
        val appContext = activity.applicationContext
        val rewardedUnitId = resolveRewardedAdUnitId()
        val adRequest = AdRequest.Builder().build()

        runCatching {
            RewardedAd.load(
                appContext,
                rewardedUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        callbacks.onLoadingChanged(false)
                        val hostActivity = activityRef.get()
                        if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
                            rewardedRequestInFlight.set(false)
                            callbacks.onDismissedWithoutReward(sessionId)
                            return
                        }

                        val earnedThisShow = AtomicBoolean(false)
                        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd.fullScreenContentCallback = null
                                rewardedRequestInFlight.set(false)
                                if (!earnedThisShow.get()) {
                                    callbacks.onDismissedWithoutReward(sessionId)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                rewardedAd.fullScreenContentCallback = null
                                rewardedRequestInFlight.set(false)
                                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                                callbacks.onFailed(
                                    sessionId,
                                    "Could not display rewarded ad right now. Please try again."
                                )
                            }
                        }

                        rewardedAd.show(hostActivity) { _ ->
                            if (earnedThisShow.compareAndSet(false, true)) {
                                callbacks.onRewardEarned(sessionId)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedRequestInFlight.set(false)
                        callbacks.onLoadingChanged(false)
                        Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                        callbacks.onFailed(
                            sessionId,
                            "Rewarded ad could not load right now. Please try again."
                        )
                    }
                }
            )
        }.onFailure { error ->
            rewardedRequestInFlight.set(false)
            callbacks.onLoadingChanged(false)
            Log.w(TAG, "Rewarded ad exception: ${error.localizedMessage}")
            callbacks.onFailed(
                sessionId,
                "Unable to load rewarded ad right now. Please try again."
            )
        }
    }
}

@Composable
fun rememberIsInternetConnected(): Boolean {
    val context = LocalContext.current
    val flow = remember(context) {
        FazaAdsManager.observeInternetConnectivity(context)
    }
    val initialValue = remember(context) {
        FazaAdsManager.isInternetConnected(context)
    }
    val connected by flow.collectAsStateWithLifecycle(initialValue = initialValue)
    return connected
}
