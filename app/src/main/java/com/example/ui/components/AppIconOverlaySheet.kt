package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppIconLanguageLayout
import com.example.data.model.AppIconOverlayAnimation
import com.example.data.model.AppIconOverlayConfig
import com.example.data.model.AppIconOverlayPosition
import com.example.data.model.AppIconOverlayStyle
import com.example.data.model.AppIconOverlayTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

fun resolveAppIconAnimationDurationMs(baseDurationMs: Int, speedMultiplier: Float): Int {
    val safeSpeed = speedMultiplier.coerceIn(0.5f, 2.0f)
    return (baseDurationMs / safeSpeed).roundToInt().coerceAtLeast(350)
}

fun resolveAppIconOverlayBias(config: AppIconOverlayConfig): Pair<Float, Float> = when (config.position) {
    AppIconOverlayPosition.TOP_START -> -1f to -1f
    AppIconOverlayPosition.TOP_END -> 1f to -1f
    AppIconOverlayPosition.TOP_CENTER -> 0f to -1f
    AppIconOverlayPosition.BOTTOM_START -> -1f to 1f
    AppIconOverlayPosition.BOTTOM_END -> 1f to 1f
    AppIconOverlayPosition.BOTTOM_CENTER -> 0f to 1f
    AppIconOverlayPosition.CUSTOM -> {
        val hx = (config.customXFraction.coerceIn(0f, 1f) * 2f) - 1f
        val vy = (config.customYFraction.coerceIn(0f, 1f) * 2f) - 1f
        hx to vy
    }
}

fun resolveAppIconOverlayAlignment(config: AppIconOverlayConfig): Alignment {
    val (hx, vy) = resolveAppIconOverlayBias(config)
    return BiasAlignment(horizontalBias = hx, verticalBias = vy)
}

@Composable
fun AlwaysOnAppIconOverlay(
    config: AppIconOverlayConfig,
    availableWidth: Dp,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    videoLanguageLabel: String? = null
) {
    if (!config.enabled) return

    val baseIconDp = when {
        availableWidth < 360.dp -> 22f
        availableWidth < 600.dp -> 26f
        availableWidth < 840.dp -> 30f
        else -> 34f
    }
    val safeScale = config.sizeScale.coerceIn(0.65f, 1.65f)
    val safeOpacity = config.opacity.coerceIn(0.20f, 1.0f)
    val iconDp = (baseIconDp * safeScale).dp
    val labelSp = (baseIconDp * 0.44f * safeScale.coerceAtMost(1.35f)).sp
    val langSp = (baseIconDp * 0.40f * safeScale.coerceAtMost(1.35f)).sp
    val activeLanguageLabel = if (config.showVideoLanguage) {
        videoLanguageLabel?.trim()?.takeIf { it.isNotEmpty() }
    } else {
        null
    }
    val isTwoLines = config.languageLayout == AppIconLanguageLayout.TWO_LINES

    val shouldAnimateGemini = config.animation == AppIconOverlayAnimation.GEMINI_GLOW &&
        (!config.animateOnlyWhilePlaying || isPlaying)

    // Lightweight GPU-friendly Gemini animation: created ONLY when enabled & playing,
    // and sampled purely in drawWithContent to avoid per-frame recompositions.
    var geminiColorPhaseState: State<Float>? = null
    var geminiShimmerPhaseState: State<Float>? = null

    if (shouldAnimateGemini) {
        val infiniteTransition = rememberInfiniteTransition(label = "app_icon_gemini_anim")
        geminiColorPhaseState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = resolveAppIconAnimationDurationMs(4400, config.animationSpeed),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "app_icon_gemini_color_phase"
        )
        geminiShimmerPhaseState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = resolveAppIconAnimationDurationMs(3400, config.animationSpeed),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "app_icon_gemini_shimmer_phase"
        )
    }

    val palette = when (config.theme) {
        AppIconOverlayTheme.MATERIAL_YOU -> AppIconOverlayPalette(
            bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            fg = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        )
        AppIconOverlayTheme.FROSTED_DARK -> AppIconOverlayPalette(
            bg = Color(0xA60B0D12),
            fg = Color.White.copy(alpha = 0.96f),
            border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.24f))
        )
        AppIconOverlayTheme.TRANSPARENT -> AppIconOverlayPalette(
            bg = Color.Transparent,
            fg = Color.White,
            border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.35f))
        )
        AppIconOverlayTheme.HIGH_CONTRAST -> AppIconOverlayPalette(
            bg = Color(0xEB000000),
            fg = Color.White,
            border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
        )
    }

    val shape = when (config.style) {
        AppIconOverlayStyle.ROUNDED_BADGE -> RoundedCornerShape((10f * safeScale.coerceAtMost(1.3f)).dp)
        AppIconOverlayStyle.CIRCLE_BADGE -> if (activeLanguageLabel != null) {
            if (isTwoLines) RoundedCornerShape((12f * safeScale.coerceAtMost(1.3f)).dp) else RoundedCornerShape(999.dp)
        } else {
            CircleShape
        }
        AppIconOverlayStyle.PILL_BRAND -> if (activeLanguageLabel != null && isTwoLines) {
            RoundedCornerShape((14f * safeScale.coerceAtMost(1.3f)).dp)
        } else {
            RoundedCornerShape(999.dp)
        }
        AppIconOverlayStyle.ICON_ONLY -> RoundedCornerShape(8.dp)
    }

    val effectiveBg = if (config.style == AppIconOverlayStyle.ICON_ONLY) {
        Color.Transparent
    } else {
        palette.bg
    }
    val effectiveBorder = if (config.style == AppIconOverlayStyle.ICON_ONLY || !config.showBorder) {
        null
    } else {
        palette.border
    }

    val textShadow = if (effectiveBg == Color.Transparent) {
        Shadow(color = Color.Black.copy(alpha = 0.9f), offset = Offset(1f, 1f), blurRadius = 6f)
    } else {
        null
    }
    val languageTextStyle = TextStyle(
        color = palette.fg.copy(alpha = 0.92f),
        fontSize = langSp,
        fontWeight = FontWeight.SemiBold,
        shadow = textShadow
    )
    val accessibilityDescription = if (activeLanguageLabel != null) {
        "FAZA Player app icon, audio language $activeLanguageLabel"
    } else {
        "FAZA Player app icon"
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = safeOpacity
            }
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            }
            .testTag("always_on_app_icon")
    ) {
        if (config.style == AppIconOverlayStyle.ICON_ONLY) {
            val iconOnlySize = (iconDp.value * 1.2f).dp
            val iconOnlyClip = RoundedCornerShape(8.dp)
            if (activeLanguageLabel == null) {
                GeminiAnimatedAppIconEmblem(
                    size = iconOnlySize,
                    clipShape = iconOnlyClip,
                    animateGemini = shouldAnimateGemini,
                    colorPhaseState = geminiColorPhaseState,
                    shimmerPhaseState = geminiShimmerPhaseState
                )
            } else if (isTwoLines) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GeminiAnimatedAppIconEmblem(
                        size = iconOnlySize,
                        clipShape = iconOnlyClip,
                        animateGemini = shouldAnimateGemini,
                        colorPhaseState = geminiColorPhaseState,
                        shimmerPhaseState = geminiShimmerPhaseState
                    )
                    Text(
                        text = activeLanguageLabel,
                        style = languageTextStyle.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("always_on_app_icon_language")
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6f * safeScale.coerceAtMost(1.2f)).dp)
                ) {
                    GeminiAnimatedAppIconEmblem(
                        size = iconOnlySize,
                        clipShape = iconOnlyClip,
                        animateGemini = shouldAnimateGemini,
                        colorPhaseState = geminiColorPhaseState,
                        shimmerPhaseState = geminiShimmerPhaseState
                    )
                    Text(
                        text = activeLanguageLabel,
                        style = languageTextStyle.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("always_on_app_icon_language")
                    )
                }
            }
        } else {
            Surface(
                color = effectiveBg,
                shape = shape,
                border = effectiveBorder
            ) {
                when (config.style) {
                    AppIconOverlayStyle.PILL_BRAND -> {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = (9f * safeScale.coerceAtMost(1.25f)).dp,
                                vertical = (5f * safeScale.coerceAtMost(1.2f)).dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GeminiAnimatedAppIconEmblem(
                                size = (iconDp.value * 0.88f).dp,
                                clipShape = RoundedCornerShape(6.dp),
                                animateGemini = shouldAnimateGemini,
                                colorPhaseState = geminiColorPhaseState,
                                shimmerPhaseState = geminiShimmerPhaseState
                            )
                            Spacer(modifier = Modifier.width((6f * safeScale.coerceAtMost(1.2f)).dp))
                            if (activeLanguageLabel != null && isTwoLines) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "FAZA",
                                        style = TextStyle(
                                            color = palette.fg,
                                            fontSize = labelSp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.6.sp,
                                            shadow = textShadow
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.testTag("always_on_app_icon_label")
                                    )
                                    Text(
                                        text = activeLanguageLabel,
                                        style = languageTextStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("always_on_app_icon_language")
                                    )
                                }
                            } else {
                                Text(
                                    text = "FAZA",
                                    style = TextStyle(
                                        color = palette.fg,
                                        fontSize = labelSp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        shadow = textShadow
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.testTag("always_on_app_icon_label")
                                )
                                if (activeLanguageLabel != null) {
                                    Text(
                                        text = " · ",
                                        style = languageTextStyle.copy(color = palette.fg.copy(alpha = 0.6f)),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = activeLanguageLabel,
                                        style = languageTextStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("always_on_app_icon_language")
                                    )
                                }
                            }
                        }
                    }

                    AppIconOverlayStyle.CIRCLE_BADGE,
                    AppIconOverlayStyle.ROUNDED_BADGE -> {
                        val badgePadding = (6f * safeScale.coerceAtMost(1.25f)).dp
                        val emblemClip = if (config.style == AppIconOverlayStyle.CIRCLE_BADGE) {
                            CircleShape
                        } else {
                            RoundedCornerShape((6f * safeScale.coerceAtMost(1.25f)).dp)
                        }
                        if (activeLanguageLabel == null) {
                            Box(
                                modifier = Modifier.padding(badgePadding),
                                contentAlignment = Alignment.Center
                            ) {
                                GeminiAnimatedAppIconEmblem(
                                    size = iconDp,
                                    clipShape = emblemClip,
                                    animateGemini = shouldAnimateGemini,
                                    colorPhaseState = geminiColorPhaseState,
                                    shimmerPhaseState = geminiShimmerPhaseState
                                )
                            }
                        } else if (isTwoLines) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = (8f * safeScale.coerceAtMost(1.25f)).dp,
                                    vertical = (5f * safeScale.coerceAtMost(1.2f)).dp
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                GeminiAnimatedAppIconEmblem(
                                    size = (iconDp.value * 0.92f).dp,
                                    clipShape = emblemClip,
                                    animateGemini = shouldAnimateGemini,
                                    colorPhaseState = geminiColorPhaseState,
                                    shimmerPhaseState = geminiShimmerPhaseState
                                )
                                Text(
                                    text = activeLanguageLabel,
                                    style = languageTextStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("always_on_app_icon_language")
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(
                                    start = (6f * safeScale.coerceAtMost(1.25f)).dp,
                                    end = (9f * safeScale.coerceAtMost(1.25f)).dp,
                                    top = (5f * safeScale.coerceAtMost(1.2f)).dp,
                                    bottom = (5f * safeScale.coerceAtMost(1.2f)).dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((6f * safeScale.coerceAtMost(1.2f)).dp)
                            ) {
                                GeminiAnimatedAppIconEmblem(
                                    size = (iconDp.value * 0.88f).dp,
                                    clipShape = emblemClip,
                                    animateGemini = shouldAnimateGemini,
                                    colorPhaseState = geminiColorPhaseState,
                                    shimmerPhaseState = geminiShimmerPhaseState
                                )
                                Text(
                                    text = activeLanguageLabel,
                                    style = languageTextStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("always_on_app_icon_language")
                                )
                            }
                        }
                    }

                    AppIconOverlayStyle.ICON_ONLY -> Unit
                }
            }
        }
    }
}

private val GeminiGlowSpectrum = listOf(
    Color(0xFF4285F4), // Gemini Blue
    Color(0xFF6B7CFF), // Periwinkle Indigo
    Color(0xFF9B72CB), // Gemini Violet
    Color(0xFFD96570), // Soft Rose
    Color(0xFF38BDF8)  // Luminous Cyan
)

internal fun sampleGeminiGlowSpectrum(phase: Float): Color {
    val wrapped = ((phase % 1f) + 1f) % 1f
    val count = GeminiGlowSpectrum.size
    val scaled = wrapped * count
    val index = scaled.toInt().coerceIn(0, count - 1)
    val nextIndex = (index + 1) % count
    val fraction = scaled - index
    return lerp(GeminiGlowSpectrum[index], GeminiGlowSpectrum[nextIndex], fraction)
}

@Composable
private fun GeminiAnimatedAppIconEmblem(
    size: Dp,
    clipShape: Shape,
    animateGemini: Boolean,
    colorPhaseState: State<Float>?,
    shimmerPhaseState: State<Float>?,
    modifier: Modifier = Modifier
) {
    val geminiModifier = if (animateGemini && colorPhaseState != null && shimmerPhaseState != null) {
        Modifier.drawWithContent {
            val colorPhase = colorPhaseState.value
            val shimmerPhase = shimmerPhaseState.value
            val primaryGlow = sampleGeminiGlowSpectrum(colorPhase)
            val secondaryGlow = sampleGeminiGlowSpectrum(colorPhase + 0.34f)
            // Restrained gentle breath [0.78, 1.0] — never bright, flashing, or distracting
            val breath = 0.89f + (0.11f * sin(shimmerPhase * 2.0 * PI).toFloat())

            // 1. Subtle soft ambient Gemini halo behind the static icon
            val glowRadius = (this.size.maxDimension * 0.84f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to primaryGlow.copy(alpha = 0.28f * breath),
                        0.55f to secondaryGlow.copy(alpha = 0.14f * breath),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Crisp static App Icon artwork (zero bounce, spin, or scale motion)
            drawContent()

            // 3. Soft diagonal gradient shimmer + fluid Gemini spectrum rim clipped cleanly to icon shape
            val outline = clipShape.createOutline(this.size, layoutDirection, this)
            val outlinePath = Path().apply { addOutline(outline) }
            clipPath(outlinePath) {
                val diag = this.size.width + this.size.height
                val sweepPos = (-0.55f + shimmerPhase * 2.1f) * diag
                val bandSpan = (diag * 0.40f).coerceAtLeast(1f)
                val shimmerBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.32f to primaryGlow.copy(alpha = 0.10f),
                        0.50f to Color.White.copy(alpha = 0.22f),
                        0.68f to secondaryGlow.copy(alpha = 0.12f),
                        1.0f to Color.Transparent
                    ),
                    start = Offset(sweepPos - bandSpan, sweepPos - bandSpan),
                    end = Offset(sweepPos + bandSpan, sweepPos + bandSpan)
                )
                drawRect(brush = shimmerBrush)

                val rimColors = listOf(
                    sampleGeminiGlowSpectrum(colorPhase).copy(alpha = 0.60f * breath),
                    sampleGeminiGlowSpectrum(colorPhase + 0.20f).copy(alpha = 0.54f * breath),
                    sampleGeminiGlowSpectrum(colorPhase + 0.40f).copy(alpha = 0.60f * breath),
                    sampleGeminiGlowSpectrum(colorPhase + 0.60f).copy(alpha = 0.54f * breath),
                    sampleGeminiGlowSpectrum(colorPhase + 0.80f).copy(alpha = 0.60f * breath),
                    sampleGeminiGlowSpectrum(colorPhase).copy(alpha = 0.60f * breath)
                )
                drawPath(
                    path = outlinePath,
                    brush = Brush.sweepGradient(colors = rimColors, center = center),
                    style = Stroke(width = 1.35.dp.toPx())
                )
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(geminiModifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.faza_icon_fg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(clipShape)
        )
    }
}

private data class AppIconOverlayPalette(
    val bg: Color,
    val fg: Color,
    val border: BorderStroke
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppIconOverlayStyleEditor(
    config: AppIconOverlayConfig,
    onConfigChange: (AppIconOverlayConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizePresets = listOf(
        "Small" to 0.78f,
        "Medium" to 1.0f,
        "Large" to 1.25f,
        "XL" to 1.50f
    )
    val opacityPresets = listOf(
        "40%" to 0.40f,
        "70%" to 0.70f,
        "85%" to 0.85f,
        "100%" to 1.0f
    )

    val previewLanguageSamples = listOf("हिंदी", "English", "বাংলা")
    var previewLanguageSample by remember { mutableStateOf(previewLanguageSamples.first()) }

    val currentConfig by rememberUpdatedState(config)
    val currentOnConfigChange by rememberUpdatedState(onConfigChange)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Interactive Live Placement Theater Preview
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0E131F), Color(0xFF1A2236), Color(0xFF0E131F))
                    )
                )
                .pointerInput(config.enabled) {
                    if (!config.enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val xFrac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val yFrac = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnConfigChange(
                            currentConfig.copy(
                                position = AppIconOverlayPosition.CUSTOM,
                                customXFraction = xFrac,
                                customYFraction = yFrac
                            )
                        )
                    }
                }
                .pointerInput(config.enabled) {
                    if (!config.enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val xFrac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val yFrac = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        currentOnConfigChange(
                            currentConfig.copy(
                                position = AppIconOverlayPosition.CUSTOM,
                                customXFraction = xFrac,
                                customYFraction = yFrac
                            )
                        )
                    }
                }
                .testTag("app_icon_preview_canvas")
        ) {
            val previewWidth = maxWidth
            if (config.enabled) {
                val (_, verticalBias) = resolveAppIconOverlayBias(config)
                val hintAlignment = if (verticalBias > 0.25f) Alignment.TopEnd else Alignment.BottomEnd
                val hintText = if (config.position == AppIconOverlayPosition.CUSTOM) {
                    "Custom (${(config.customXFraction * 100).roundToInt()}%, ${(config.customYFraction * 100).roundToInt()}%) · Drag to place"
                } else {
                    "${config.position.label} Preview · Tap or drag to customize"
                }

                Text(
                    text = hintText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                    modifier = Modifier
                        .align(hintAlignment)
                        .padding(12.dp)
                        .testTag("app_icon_placement_hint")
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    AlwaysOnAppIconOverlay(
                        config = config,
                        availableWidth = previewWidth,
                        videoLanguageLabel = previewLanguageSample,
                        modifier = Modifier.align(resolveAppIconOverlayAlignment(config))
                    )
                }
            } else {
                Text(
                    text = "Permanent App Icon is turned OFF (Optional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Optional On / Off Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text("Always Show App Icon", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Keep the FAZA Player icon permanently visible in the playing UI (optional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                modifier = Modifier.testTag("app_icon_enabled_switch")
            )
        }

        if (config.enabled) {
            // Customized Placement Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenWith,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Placement", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = if (config.position == AppIconOverlayPosition.CUSTOM) {
                            "${(config.customXFraction * 100).roundToInt()}% · ${(config.customYFraction * 100).roundToInt()}%"
                        } else {
                            config.position.label
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("app_icon_placement_summary")
                    )
                }
                Text(
                    text = "Pick any corner, center edge, or choose Custom to place the icon anywhere on screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppIconOverlayPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = config.position == pos,
                            onClick = { onConfigChange(config.copy(position = pos)) },
                            label = { Text(pos.label) },
                            modifier = Modifier.testTag("app_icon_position_${pos.name.lowercase()}")
                        )
                    }
                }

                AnimatedVisibility(visible = config.position == AppIconOverlayPosition.CUSTOM) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("app_icon_custom_placement_controls")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Horizontal Position", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${(config.customXFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.customXFraction.coerceIn(0f, 1f),
                                onValueChange = {
                                    onConfigChange(
                                        config.copy(
                                            position = AppIconOverlayPosition.CUSTOM,
                                            customXFraction = it.coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.testTag("app_icon_custom_x_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vertical Position", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${(config.customYFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = config.customYFraction.coerceIn(0f, 1f),
                                onValueChange = {
                                    onConfigChange(
                                        config.copy(
                                            position = AppIconOverlayPosition.CUSTOM,
                                            customYFraction = it.coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.testTag("app_icon_custom_y_slider")
                            )

                            FilledTonalButton(
                                onClick = {
                                    onConfigChange(
                                        config.copy(
                                            position = AppIconOverlayPosition.TOP_START,
                                            customXFraction = 0.08f,
                                            customYFraction = 0.12f
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("app_icon_reset_placement_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset to Top Left")
                            }
                        }
                    }
                }
            }

            // Optional Icon Animation
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val speedPresets = listOf(
                    "Slow" to 0.65f,
                    "Normal" to 1.0f,
                    "Fast" to 1.5f,
                    "Max" to 2.0f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Animation (Optional)", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = config.animation.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("app_icon_animation_summary")
                    )
                }
                Text(
                    text = "Keep the icon static (Off) or enable a subtle Gemini-style glow and gradient shimmer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppIconOverlayAnimation.entries.forEach { anim ->
                        FilterChip(
                            selected = config.animation == anim,
                            onClick = { onConfigChange(config.copy(animation = anim)) },
                            label = { Text(anim.label) },
                            modifier = Modifier.testTag("app_icon_anim_${anim.name.lowercase()}")
                        )
                    }
                }

                AnimatedVisibility(visible = config.animation != AppIconOverlayAnimation.NONE) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("app_icon_animation_controls")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Animation Speed", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${"%.2f".format(config.animationSpeed.coerceIn(0.5f, 2.0f)).trimEnd('0').trimEnd('.')}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                speedPresets.forEach { (label, preset) ->
                                    FilterChip(
                                        selected = abs(config.animationSpeed - preset) < 0.08f,
                                        onClick = { onConfigChange(config.copy(animationSpeed = preset)) },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("app_icon_anim_speed_${label.lowercase()}")
                                    )
                                }
                            }
                            Slider(
                                value = config.animationSpeed.coerceIn(0.5f, 2.0f),
                                onValueChange = {
                                    onConfigChange(config.copy(animationSpeed = it.coerceIn(0.5f, 2.0f)))
                                },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.testTag("app_icon_anim_speed_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Text("Pause When Video Pauses", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "Rest the icon animation automatically when playback is paused",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = config.animateOnlyWhilePlaying,
                                    onCheckedChange = {
                                        onConfigChange(config.copy(animateOnlyWhilePlaying = it))
                                    },
                                    modifier = Modifier.testTag("app_icon_anim_only_playing_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Optional Video Language Display (One Line / Two Lines)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Show Video Language (Optional)", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "Show the active video language with the app icon (e.g. हिंदी, English, বাংলা)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.showVideoLanguage,
                        onCheckedChange = { onConfigChange(config.copy(showVideoLanguage = it)) },
                        modifier = Modifier.testTag("app_icon_language_switch")
                    )
                }

                AnimatedVisibility(visible = config.showVideoLanguage) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .testTag("app_icon_language_controls")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Language Layout", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = config.languageLayout.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("app_icon_lang_layout_summary")
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppIconLanguageLayout.entries.forEach { layout ->
                                    FilterChip(
                                        selected = config.languageLayout == layout,
                                        onClick = { onConfigChange(config.copy(languageLayout = layout)) },
                                        label = { Text(layout.label) },
                                        modifier = Modifier.testTag("app_icon_lang_layout_${layout.name.lowercase()}")
                                    )
                                }
                            }
                            Text(
                                text = "Preview Language",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                previewLanguageSamples.forEach { sample ->
                                    FilterChip(
                                        selected = previewLanguageSample == sample,
                                        onClick = { previewLanguageSample = sample },
                                        label = { Text(sample) },
                                        modifier = Modifier.testTag("app_icon_lang_preview_$sample")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Icon Style
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Icon Style", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppIconOverlayStyle.entries.forEach { style ->
                        FilterChip(
                            selected = config.style == style,
                            onClick = { onConfigChange(config.copy(style = style)) },
                            label = { Text(style.label) },
                            modifier = Modifier.testTag("app_icon_style_${style.name.lowercase()}")
                        )
                    }
                }
            }

            // Theme (applicable when badge container is shown)
            if (config.style != AppIconOverlayStyle.ICON_ONLY) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Badge Theme", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppIconOverlayTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = config.theme == theme,
                                onClick = { onConfigChange(config.copy(theme = theme)) },
                                label = { Text(theme.label) },
                                modifier = Modifier.testTag("app_icon_theme_${theme.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Icon Size
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Icon Size", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${(config.sizeScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sizePresets.forEach { (label, preset) ->
                        FilterChip(
                            selected = abs(config.sizeScale - preset) < 0.06f,
                            onClick = { onConfigChange(config.copy(sizeScale = preset)) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("app_icon_size_${label.lowercase()}")
                        )
                    }
                }
                Slider(
                    value = config.sizeScale.coerceIn(0.65f, 1.65f),
                    onValueChange = { onConfigChange(config.copy(sizeScale = it.coerceIn(0.65f, 1.65f))) },
                    valueRange = 0.65f..1.65f,
                    modifier = Modifier.testTag("app_icon_size_slider")
                )
            }

            // Watermark Opacity
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opacity", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${(config.opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    opacityPresets.forEach { (label, preset) ->
                        FilterChip(
                            selected = abs(config.opacity - preset) < 0.04f,
                            onClick = { onConfigChange(config.copy(opacity = preset)) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("app_icon_opacity_${(preset * 100).roundToInt()}")
                        )
                    }
                }
                Slider(
                    value = config.opacity.coerceIn(0.20f, 1.0f),
                    onValueChange = { onConfigChange(config.copy(opacity = it.coerceIn(0.20f, 1.0f))) },
                    valueRange = 0.20f..1.0f,
                    modifier = Modifier.testTag("app_icon_opacity_slider")
                )
            }

            // Outline Border Option
            if (config.style != AppIconOverlayStyle.ICON_ONLY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text("Outline Border", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Add a crisp edge border around the app icon badge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.showBorder,
                        onCheckedChange = { onConfigChange(config.copy(showBorder = it)) },
                        modifier = Modifier.testTag("app_icon_border_switch")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconOverlayBottomSheet(
    config: AppIconOverlayConfig,
    onConfigChange: (AppIconOverlayConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Permanent App Icon",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AppIconOverlayStyleEditor(config = config, onConfigChange = onConfigChange)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
