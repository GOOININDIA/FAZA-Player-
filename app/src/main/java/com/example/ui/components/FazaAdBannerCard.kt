package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ads.FazaAdsManager
import com.example.ui.theme.FazaNavBorderDark
import com.example.ui.theme.FazaNavBorderLight
import com.example.ui.theme.FazaNavSurfaceDark
import com.example.ui.theme.FazaNavSurfaceLight
import com.example.ui.theme.LocalFazaIsDark
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun FazaAdBannerCard(
    testTag: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = LocalFazaIsDark.current
    val isRobolectric = remember { FazaAdsManager.isRobolectricRuntime() }
    val bannerAdUnitId = remember { FazaAdsManager.resolveBannerAdUnitId() }
    var loadFailed by remember(bannerAdUnitId) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FazaAdsManager.ensureInitializedAsync(context)
    }

    AnimatedVisibility(
        visible = !loadFailed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        val surfaceColor = if (isDark) FazaNavSurfaceDark else FazaNavSurfaceLight
        val borderColor = if (isDark) FazaNavBorderDark else FazaNavBorderLight
        val adDescription = stringResource(R.string.ad_test_banner_cd)

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics { contentDescription = adDescription }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidthDp = maxWidth.value.toInt().coerceAtLeast(320)

                if (isRobolectric) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.ad_test_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.ad_test_banner_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val adView = remember(availableWidthDp, bannerAdUnitId) {
                        AdView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            adUnitId = bannerAdUnitId
                            @Suppress("DEPRECATION")
                            setAdSize(
                                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                    context,
                                    availableWidthDp
                                )
                            )
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    loadFailed = false
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    loadFailed = true
                                }
                            }
                            if (FazaAdsManager.isInternetConnected(context)) {
                                loadAd(AdRequest.Builder().build())
                            } else {
                                loadFailed = true
                            }
                        }
                    }

                    DisposableEffect(lifecycleOwner, adView) {
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
                            runCatching {
                                adView.adListener = object : AdListener() {}
                                adView.destroy()
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(min = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { adView },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
