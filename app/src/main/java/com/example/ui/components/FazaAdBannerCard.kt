package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.FazaAdsManager
import com.example.ads.rememberIsInternetConnected
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private enum class BannerLoadStatus {
    LOADING,
    LOADED,
    FAILED
}

@Composable
fun FazaAdBannerCard(
    modifier: Modifier = Modifier,
    testTag: String = "faza_ad_banner_card",
    dockedBottom: Boolean = false
) {
    val isInternetConnected = rememberIsInternetConnected()
    if (!isInternetConnected) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTestMode = remember { FazaAdsManager.isUsingTestAdIds() }
    val adUnitId = remember { FazaAdsManager.resolveBannerAdUnitId() }
    val isRobolectric = remember { FazaAdsManager.isRobolectricRuntime() }

    var loadStatus by remember(isInternetConnected) {
        mutableStateOf(if (isRobolectric) BannerLoadStatus.LOADED else BannerLoadStatus.LOADING)
    }
    var statusMessage by remember(isInternetConnected) { mutableStateOf<String?>(null) }
    var suppressOfflineFailure by remember(isInternetConnected) { mutableStateOf(false) }

    LaunchedEffect(context, isInternetConnected) {
        if (isInternetConnected) {
            FazaAdsManager.initialize(context)
        }
    }

    if (suppressOfflineFailure) {
        return
    }

    FazaGlassCard(
        shape = RoundedCornerShape(if (dockedBottom) 22.dp else 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = if (dockedBottom) 10.dp else 12.dp,
            vertical = if (dockedBottom) 6.dp else 10.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (dockedBottom) 4.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isTestMode) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isTestMode) "TEST AD" else "AD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTestMode) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(
                                horizontal = if (dockedBottom) 6.dp else 8.dp,
                                vertical = 1.dp
                            )
                        )
                    }
                    Text(
                        text = if (isTestMode) {
                            "Google AdMob Test Banner"
                        } else {
                            "Sponsored"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = "Advertisement",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (dockedBottom) 14.dp else 16.dp)
                )
            }

            if (isRobolectric) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTestMode) {
                            "Adaptive Test Banner (${adUnitId.takeLast(10)})"
                        } else {
                            "Adaptive AdMob Banner (${adUnitId.takeLast(10)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val widthDp = maxWidth.value.toInt().coerceAtLeast(300)
                    val adaptiveAdSize = remember(context, widthDp) {
                        runCatching {
                            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                context,
                                widthDp
                            )
                        }.getOrElse { AdSize.BANNER }
                    }

                    val adView = remember(context, adUnitId, adaptiveAdSize) {
                        runCatching {
                            AdView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setAdSize(adaptiveAdSize)
                                this.adUnitId = adUnitId
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        loadStatus = BannerLoadStatus.LOADED
                                        statusMessage = null
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        loadStatus = BannerLoadStatus.FAILED
                                        if (
                                            error.code == AdRequest.ERROR_CODE_NETWORK_ERROR ||
                                            !FazaAdsManager.isInternetConnected(context)
                                        ) {
                                            suppressOfflineFailure = true
                                            statusMessage = null
                                        } else {
                                            statusMessage = "Ad unavailable (${error.code})"
                                        }
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }.getOrElse { error ->
                            loadStatus = BannerLoadStatus.FAILED
                            statusMessage = error.localizedMessage ?: "Ad SDK unavailable on this device"
                            null
                        }
                    }

                    DisposableEffect(lifecycleOwner, adView) {
                        if (adView == null) {
                            onDispose { }
                        } else {
                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> runCatching { adView.resume() }
                                    Lifecycle.Event.ON_PAUSE -> runCatching { adView.pause() }
                                    else -> Unit
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                runCatching { adView.destroy() }
                            }
                        }
                    }

                    if (adView != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AndroidView(
                                factory = { adView },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                            )
                            if (loadStatus == BannerLoadStatus.LOADING) {
                                Text(
                                    text = if (isTestMode) "Loading Google test ad..." else "Loading ad...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (loadStatus == BannerLoadStatus.FAILED && statusMessage != null) {
                                Text(
                                    text = statusMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (statusMessage != null) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
