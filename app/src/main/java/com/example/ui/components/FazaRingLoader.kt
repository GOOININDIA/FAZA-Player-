// From Uiverse.io by Nawsome - https://uiverse.io/Nawsome/young-goat-78
// MIT License, Copyright (c) Nawsome

package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val LOADER_VIEWBOX_SIZE = 240f
private const val LOADER_CYCLE_DURATION_MS = 2_000

private val RingAColor = Color(0xFFF42F25)
private val RingBColor = Color(0xFFF49725)
private val RingCColor = Color(0xFF255FF4)
private val RingDColor = Color(0xFFF42582)

internal data class RingLoaderKeyframe(
    val fraction: Float,
    val dashLength: Float,
    val strokeWidth: Float,
    val dashOffset: Float
)

internal data class RingLoaderSample(
    val dashLength: Float,
    val strokeWidth: Float,
    val dashOffset: Float
)

internal data class RingLoaderSpec(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val circumference: Float,
    val color: Color,
    val keyframes: List<RingLoaderKeyframe>
)

internal val FazaRingLoaderSpecs: List<RingLoaderSpec> = listOf(
    // Ring A (outer)
    RingLoaderSpec(
        cx = 120f,
        cy = 120f,
        radius = 105f,
        circumference = 660f,
        color = RingAColor,
        keyframes = listOf(
            RingLoaderKeyframe(0.00f, 0f, 20f, -330f),
            RingLoaderKeyframe(0.04f, 0f, 20f, -330f),
            RingLoaderKeyframe(0.12f, 60f, 30f, -335f),
            RingLoaderKeyframe(0.32f, 60f, 30f, -595f),
            RingLoaderKeyframe(0.40f, 0f, 20f, -660f),
            RingLoaderKeyframe(0.54f, 0f, 20f, -660f),
            RingLoaderKeyframe(0.62f, 60f, 30f, -665f),
            RingLoaderKeyframe(0.82f, 60f, 30f, -925f),
            RingLoaderKeyframe(0.90f, 0f, 20f, -990f),
            RingLoaderKeyframe(1.00f, 0f, 20f, -990f)
        )
    ),
    // Ring B (inner)
    RingLoaderSpec(
        cx = 120f,
        cy = 120f,
        radius = 35f,
        circumference = 220f,
        color = RingBColor,
        keyframes = listOf(
            RingLoaderKeyframe(0.00f, 0f, 20f, -110f),
            RingLoaderKeyframe(0.12f, 0f, 20f, -110f),
            RingLoaderKeyframe(0.20f, 20f, 30f, -115f),
            RingLoaderKeyframe(0.40f, 20f, 30f, -195f),
            RingLoaderKeyframe(0.48f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.62f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.70f, 20f, 30f, -225f),
            RingLoaderKeyframe(0.90f, 20f, 30f, -305f),
            RingLoaderKeyframe(0.98f, 0f, 20f, -330f),
            RingLoaderKeyframe(1.00f, 0f, 20f, -330f)
        )
    ),
    // Ring C (left)
    RingLoaderSpec(
        cx = 85f,
        cy = 120f,
        radius = 70f,
        circumference = 440f,
        color = RingCColor,
        keyframes = listOf(
            RingLoaderKeyframe(0.00f, 0f, 20f, 0f),
            RingLoaderKeyframe(0.08f, 40f, 30f, -5f),
            RingLoaderKeyframe(0.28f, 40f, 30f, -175f),
            RingLoaderKeyframe(0.36f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.58f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.66f, 40f, 30f, -225f),
            RingLoaderKeyframe(0.86f, 40f, 30f, -395f),
            RingLoaderKeyframe(0.94f, 0f, 20f, -440f),
            RingLoaderKeyframe(1.00f, 0f, 20f, -440f)
        )
    ),
    // Ring D (right)
    RingLoaderSpec(
        cx = 155f,
        cy = 120f,
        radius = 70f,
        circumference = 440f,
        color = RingDColor,
        keyframes = listOf(
            RingLoaderKeyframe(0.00f, 0f, 20f, 0f),
            RingLoaderKeyframe(0.08f, 0f, 20f, 0f),
            RingLoaderKeyframe(0.16f, 40f, 30f, -5f),
            RingLoaderKeyframe(0.36f, 40f, 30f, -175f),
            RingLoaderKeyframe(0.44f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.50f, 0f, 20f, -220f),
            RingLoaderKeyframe(0.58f, 40f, 30f, -225f),
            RingLoaderKeyframe(0.78f, 40f, 30f, -395f),
            RingLoaderKeyframe(0.86f, 0f, 20f, -440f),
            RingLoaderKeyframe(1.00f, 0f, 20f, -440f)
        )
    )
)

internal fun sampleRingLoaderState(
    keyframes: List<RingLoaderKeyframe>,
    progress: Float
): RingLoaderSample {
    val clamped = progress.coerceIn(0f, 1f)
    if (keyframes.isEmpty()) return RingLoaderSample(0f, 20f, 0f)
    if (clamped <= keyframes.first().fraction) {
        val first = keyframes.first()
        return RingLoaderSample(first.dashLength, first.strokeWidth, first.dashOffset)
    }
    for (i in 0 until keyframes.lastIndex) {
        val start = keyframes[i]
        val end = keyframes[i + 1]
        if (clamped <= end.fraction) {
            val span = end.fraction - start.fraction
            val t = if (span <= 0f) 1f else ((clamped - start.fraction) / span).coerceIn(0f, 1f)
            return RingLoaderSample(
                dashLength = start.dashLength + (end.dashLength - start.dashLength) * t,
                strokeWidth = start.strokeWidth + (end.strokeWidth - start.strokeWidth) * t,
                dashOffset = start.dashOffset + (end.dashOffset - start.dashOffset) * t
            )
        }
    }
    val last = keyframes.last()
    return RingLoaderSample(last.dashLength, last.strokeWidth, last.dashOffset)
}

@Composable
fun FazaRingLoader(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Loading",
    useThemeColors: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val themeRingColors = listOf(
        colorScheme.primary,
        colorScheme.tertiary,
        colorScheme.secondary,
        colorScheme.primary.copy(alpha = 0.72f)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "faza_ring_loader")
    val progressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOADER_CYCLE_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "faza_ring_loader_progress"
    )

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(96.dp)
            .then(semanticsModifier)
    ) {
        val drawDimension = min(size.width, size.height)
        if (drawDimension <= 0f) return@Canvas
        val scale = drawDimension / LOADER_VIEWBOX_SIZE
        val offsetX = (size.width - drawDimension) * 0.5f
        val offsetY = (size.height - drawDimension) * 0.5f
        val phase = progressState.value

        FazaRingLoaderSpecs.forEachIndexed { index, ring ->
            val sample = sampleRingLoaderState(ring.keyframes, phase)
            val startAngle = ((-sample.dashOffset) / ring.circumference) * 360f
            val sweepAngle = ((sample.dashLength / ring.circumference) * 360f).coerceAtLeast(0.1f)
            val diameter = ring.radius * 2f * scale
            val topLeft = Offset(
                x = offsetX + (ring.cx - ring.radius) * scale,
                y = offsetY + (ring.cy - ring.radius) * scale
            )
            val ringColor = if (useThemeColors) {
                themeRingColors[index % themeRingColors.size]
            } else {
                ring.color
            }

            drawArc(
                color = ringColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(
                    width = sample.strokeWidth * scale,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
