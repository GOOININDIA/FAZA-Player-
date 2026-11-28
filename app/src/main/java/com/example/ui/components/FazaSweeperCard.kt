// From Uiverse.io by rmdhnkhsn - https://uiverse.io/rmdhnkhsn/horrible-firefox-67
// MIT License, Copyright (c) rmdhnkhsn (Ramadhon Ikhsan Prasetya)

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalFazaIsDark
import kotlin.math.hypot
import kotlin.math.max

/**
 * Scoped design tokens adapted from Uiverse.io rmdhnkhsn/horrible-firefox-67 ("sweeperCard").
 */
object FazaSweeperCardTokens {
    val EmeraldAccent = Color(0xFF23C55E)
    val IconBgLight = Color(0xFFE5F6EB)
    val IconBgDark = Color(0x2E23C55E)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF161C19)
    val BorderLight = Color(0xFFE4EFE8)
    val BorderDark = Color(0x3323C55E)
    val TitleLight = Color(0xFF171D29)
    val TitleDark = Color(0xFFF2F7F4)
    val SubtitleLight = Color(0xFF7E8882)
    val SubtitleDark = Color(0xFFA0AFA6)
    val LinkLight = Color(0xFF072713)
    val LinkDark = Color(0xFFB8F5CE)
    val ShadowTintLight = Color(0x33C0C0C0)
    val DefaultShape: Shape = RoundedCornerShape(26.dp)
}

/**
 * Exposes the active emerald sweep progress (0f resting -> 1f swept green) to child composables
 * so icon tiles, badges, and text smoothly transition to white when hovered or pressed.
 */
val LocalFazaSweeperSweepFraction = compositionLocalOf { 0f }

internal fun resolveSweeperTextColor(
    restingColor: Color,
    sweepFraction: Float,
    sweptColor: Color = Color.White
): Color = lerp(restingColor, sweptColor, sweepFraction.coerceIn(0f, 1f))

internal fun resolveSweeperIconContainerColor(
    isDark: Boolean,
    sweepFraction: Float,
    accentColor: Color = FazaSweeperCardTokens.EmeraldAccent,
    sweptContentColor: Color = Color.White
): Color {
    val resting = accentColor.copy(alpha = if (isDark) 0.18f else 0.13f)
    val active = sweptContentColor.copy(alpha = 0.20f)
    return lerp(resting, active, sweepFraction.coerceIn(0f, 1f))
}

internal fun resolveSweeperIconTintColor(
    sweepFraction: Float,
    accentColor: Color = FazaSweeperCardTokens.EmeraldAccent,
    sweptContentColor: Color = Color.White
): Color = lerp(accentColor, sweptContentColor, sweepFraction.coerceIn(0f, 1f))

/**
 * Adaptive Jetpack Compose port of the Uiverse.io sweeperCard container, dynamically driven by
 * the active [MaterialTheme.colorScheme] accent so every card adopts the user's chosen theme color.
 */
@Composable
fun FazaSweeperCard(
    modifier: Modifier = Modifier,
    shape: Shape = FazaSweeperCardTokens.DefaultShape,
    accented: Boolean = false,
    highlightedBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalFazaIsDark.current
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    val accentColor = colors.primary
    val sweptContentColor = colors.onPrimary

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isSwept = isHovered || isPressed

    val sweepFraction by animateFloatAsState(
        targetValue = if (isSwept) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isHovered && !isPressed) 750 else 360,
            easing = FastOutSlowInEasing
        ),
        label = "faza_sweeper_card_sweep"
    )

    val pressScale = if (onClick != null) {
        rememberFazaBounceScale(
            interactionSource = interactionSource,
            enabled = true,
            pressedScale = 0.985f
        )
    } else {
        1f
    }

    val baseSurfaceColor = if (isDark) colors.surfaceContainerLow else colors.surfaceContainerLowest
    val restingContentColor = colors.onSurface
    val animatedContentColor = resolveSweeperTextColor(
        restingColor = restingContentColor,
        sweepFraction = sweepFraction,
        sweptColor = sweptContentColor
    )

    val targetBorderColor = when {
        isSwept || highlightedBorder -> accentColor
        accented -> accentColor.copy(alpha = if (isDark) 0.42f else 0.36f)
        else -> colors.outlineVariant.copy(alpha = if (isDark) 0.72f else 0.85f)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "faza_sweeper_card_border"
    )
    val borderWidth = if (highlightedBorder || isSwept) 1.25.dp else 1.dp

    val interactionModifier = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = sweptContentColor),
                onClick = onClick
            )
    } else {
        Modifier
            .clip(shape)
            .hoverable(interactionSource = interactionSource)
    }

    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = animatedContentColor,
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 3.dp else 5.dp,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier.then(interactionModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 1. Crisp themed card base surface
                    drawRect(color = baseSurfaceColor)

                    // 2. Ambient bottom-right theme-accent radial glow (CSS ::after)
                    val ambientRadius = max(size.width, size.height) * 0.72f
                    if (ambientRadius > 0f) {
                        val glowBoost = if (accented) 0.06f else 0f
                        val centerAlpha = (if (isDark) 0.18f else 0.16f) + glowBoost
                        val midAlpha = (if (isDark) 0.07f else 0.06f) + glowBoost * 0.4f
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to accentColor.copy(alpha = centerAlpha),
                                    0.45f to accentColor.copy(alpha = midAlpha),
                                    1.0f to Color.Transparent
                                ),
                                center = Offset(size.width * 1.02f, size.height * 1.04f),
                                radius = ambientRadius
                            )
                        )
                    }

                    // 3. Expanding theme-accent bottom-center sweep circle (CSS ::before on :hover)
                    if (sweepFraction > 0.001f) {
                        val sweepCenter = Offset(size.width * 0.5f, size.height * 1.22f)
                        val maxSweepRadius = hypot(size.width, size.height) * 1.48f
                        val currentRadius = (maxSweepRadius * sweepFraction).coerceAtLeast(1f)
                        val innerSolidStop = (0.64f + 0.34f * sweepFraction).coerceIn(0.64f, 0.98f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to accentColor,
                                    innerSolidStop to accentColor,
                                    1.0f to accentColor.copy(
                                        alpha = (sweepFraction * 0.95f).coerceIn(0f, 1f)
                                    )
                                ),
                                center = sweepCenter,
                                radius = currentRadius
                            ),
                            radius = currentRadius,
                            center = sweepCenter
                        )
                    }
                }
        ) {
            CompositionLocalProvider(
                LocalFazaSweeperSweepFraction provides sweepFraction,
                LocalContentColor provides animatedContentColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    content = content
                )
            }
        }
    }
}

/**
 * Complete structured feature card faithful to Uiverse.io rmdhnkhsn/horrible-firefox-67,
 * including the emerald-tinted icon box, bold title, subtitle, and "Learn More" action link.
 */
@Composable
fun FazaSweeperFeatureCard(
    title: String,
    subtitle: String,
    linkLabel: String = "Learn More",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (tint: Color) -> Unit = { tint ->
        FazaSweeperLayoutIcon(tint = tint, size = 28.dp)
    }
) {
    val isDark = LocalFazaIsDark.current

    FazaSweeperCard(
        modifier = modifier.widthIn(max = 340.dp),
        shape = RoundedCornerShape(30.dp),
        onClick = onClick,
        contentPadding = PaddingValues(28.dp)
    ) {
        val sweep = LocalFazaSweeperSweepFraction.current
        val iconBg = resolveSweeperIconContainerColor(isDark = isDark, sweepFraction = sweep)
        val iconTint = resolveSweeperIconTintColor(sweepFraction = sweep)
        val titleColor = resolveSweeperTextColor(
            restingColor = if (isDark) FazaSweeperCardTokens.TitleDark else FazaSweeperCardTokens.TitleLight,
            sweepFraction = sweep
        )
        val subtitleColor = resolveSweeperTextColor(
            restingColor = if (isDark) FazaSweeperCardTokens.SubtitleDark else FazaSweeperCardTokens.SubtitleLight,
            sweepFraction = sweep
        )
        val linkColor = resolveSweeperTextColor(
            restingColor = if (isDark) FazaSweeperCardTokens.LinkDark else FazaSweeperCardTokens.LinkLight,
            sweepFraction = sweep
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            iconContent(iconTint)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = linkLabel,
                color = linkColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
            FazaSweeperMoreIcon(tint = linkColor, size = 18.dp)
        }
    }
}

/**
 * Compose Canvas adaptation of the card's 28x29 rounded-panels header SVG icon.
 */
@Composable
fun FazaSweeperLayoutIcon(
    tint: Color = FazaSweeperCardTokens.EmeraldAccent,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val sx = this.size.width / 28f
        val sy = this.size.height / 29f
        val stroke = Stroke(
            width = 1.5f * ((sx + sy) * 0.5f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        // Top wide rounded rectangle
        drawRoundRect(
            color = tint,
            topLeft = Offset(4.28f * sx, 4.38f * sy),
            size = Size(19.44f * sx, 7.0f * sy),
            cornerRadius = CornerRadius(2.33f * sx, 2.33f * sy),
            style = stroke
        )
        // Bottom right compact rounded rectangle
        drawRoundRect(
            color = tint,
            topLeft = Offset(13.61f * sx, 16.82f * sy),
            size = Size(10.11f * sx, 7.0f * sy),
            cornerRadius = CornerRadius(2.33f * sx, 2.33f * sy),
            style = stroke
        )
    }
}

/**
 * Compose Canvas adaptation of the card's 18x19 dotted-frame & diagonal arrow "Learn More" SVG icon.
 */
@Composable
fun FazaSweeperMoreIcon(
    tint: Color = FazaSweeperCardTokens.LinkLight,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val sx = this.size.width / 18f
        val sy = this.size.height / 19f
        val scale = (sx + sy) * 0.5f

        val dots = listOf(
            Offset(2.75f, 2.85f),
            Offset(5.875f, 2.85f),
            Offset(9.0f, 2.85f),
            Offset(12.125f, 2.85f),
            Offset(15.25f, 2.85f),
            Offset(2.75f, 5.975f),
            Offset(15.25f, 5.975f),
            Offset(2.75f, 9.10f),
            Offset(2.75f, 12.225f),
            Offset(2.75f, 15.35f),
            Offset(5.875f, 15.35f)
        )
        for (dot in dots) {
            drawCircle(
                color = tint,
                radius = 0.75f * scale,
                center = Offset(dot.x * sx, dot.y * sy)
            )
        }

        val strokeWidth = 1.5f * scale
        // Corner arrow head at (9.25, 9.35)
        drawLine(
            color = tint,
            start = Offset(9.25f * sx, 15.35f * sy),
            end = Offset(9.25f * sx, 9.35f * sy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(9.25f * sx, 9.35f * sy),
            end = Offset(15.25f * sx, 9.35f * sy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Diagonal arrow shaft
        drawLine(
            color = tint,
            start = Offset(9.25f * sx, 9.35f * sy),
            end = Offset(15.25f * sx, 15.35f * sy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
