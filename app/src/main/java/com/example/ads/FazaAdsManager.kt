package com.example.ads

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
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FazaAdsManager"

object FazaAdsManager {

    const val GOOGLE_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val GOOGLE_TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val DEFAULT_ADMOB_APP_ID = "ca-app-pub-6572024129453689~5808434174"
    const val DEFAULT_ADMOB_BANNER_UNIT_ID = "ca-app-pub-6572024129453689/1854846916"

    private val initialized = AtomicBoolean(false)
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isRobolectricRuntime(): Boolean =
        "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    fun resolveAppId(): String =
        BuildConfig.ADMOB_APP_ID.trim().ifBlank { DEFAULT_ADMOB_APP_ID }

    fun resolveBannerAdUnitId(): String =
        BuildConfig.ADMOB_BANNER_UNIT_ID.trim().ifBlank { DEFAULT_ADMOB_BANNER_UNIT_ID }

    fun isUsingTestAdIds(): Boolean {
        val unitId = resolveBannerAdUnitId()
        val appId = resolveAppId()
        return unitId == GOOGLE_TEST_BANNER_UNIT_ID || appId == GOOGLE_TEST_APP_ID
    }

    internal fun hasValidatedInternetCapability(
        hasInternet: Boolean,
        hasValidated: Boolean,
        isRobolectric: Boolean = isRobolectricRuntime()
    ): Boolean {
        if (!hasInternet) return false
        return if (isRobolectric) true else hasValidated
    }

    fun isInternetConnected(context: Context): Boolean {
        val appContext = context.applicationContext
        return runCatching {
            val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
                ?: return@runCatching false
            val activeNetwork = connectivityManager.activeNetwork ?: return@runCatching false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?: return@runCatching false
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            hasValidatedInternetCapability(
                hasInternet = hasInternet,
                hasValidated = hasValidated
            )
        }.getOrDefault(false)
    }

    fun observeInternetConnectivity(context: Context): Flow<Boolean> {
        val appContext = context.applicationContext
        return callbackFlow {
            trySend(isInternetConnected(appContext))

            val connectivityManager = runCatching {
                appContext.getSystemService(ConnectivityManager::class.java)
            }.getOrNull()

            if (connectivityManager == null) {
                awaitClose { }
                return@callbackFlow
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(isInternetConnected(appContext))
                }

                override fun onLost(network: Network) {
                    trySend(isInternetConnected(appContext))
                }

                override fun onUnavailable() {
                    trySend(false)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val active = connectivityManager.activeNetwork
                    if (active == null || active == network) {
                        val connected = hasValidatedInternetCapability(
                            hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                            hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        )
                        trySend(connected)
                    } else {
                        trySend(isInternetConnected(appContext))
                    }
                }
            }

            val registered = runCatching {
                connectivityManager.registerDefaultNetworkCallback(callback)
                true
            }.getOrDefault(false)

            awaitClose {
                if (registered) {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }
        }.distinctUntilChanged()
    }

    fun initialize(context: Context) {
        if (isRobolectricRuntime()) return
        if (!isInternetConnected(context)) return
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
                Log.e(TAG, "Failed to initialize Google Mobile Ads SDK", error)
            }
        }
    }
}

@Composable
fun rememberIsInternetConnected(): Boolean {
    val context = LocalContext.current.applicationContext
    val connectivityFlow = remember(context) {
        FazaAdsManager.observeInternetConnectivity(context)
    }
    val initialConnected = remember(context) {
        FazaAdsManager.isInternetConnected(context)
    }
    val isConnected by connectivityFlow.collectAsStateWithLifecycle(initialValue = initialConnected)
    return isConnected
}
