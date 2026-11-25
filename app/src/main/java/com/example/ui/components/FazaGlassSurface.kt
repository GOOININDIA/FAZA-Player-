package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable spring bounce press scale animation for Material You interactive elements.
 */
@Composable
fun rememberFazaBounceScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f
): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "faza_bounce_scale"
    )
    return scale
}

/**
 * Pure Material You tonal screen backdrop driven directly by MaterialTheme.colorScheme.surface.
 */
@Composable
fun FazaAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
            content()
        }
    }
}

/**
 * Material You semantic outline brush retained for components that request an explicit outline.
 */
@Composable
fun rememberFazaGlassBorderBrush(
    highlighted: Boolean = false
): Brush {
    val colors = MaterialTheme.colorScheme
    return remember(colors, highlighted) {
        SolidColor(
            if (highlighted) {
                colors.primary
            } else {
                colors.outlineVariant.copy(alpha = 0.5f)
            }
        )
    }
}

/**
 * Material You tonal surface container brush.
 */
@Composable
fun rememberFazaGlassFillBrush(
    accented: Boolean = false
): Brush {
    val colors = MaterialTheme.colorScheme
    return remember(colors, accented) {
        SolidColor(
            if (accented) colors.secondaryContainer else colors.surfaceContainerLow
        )
    }
}

/**
 * Material You solid primary accent brush.
 */
@Composable
fun rememberFazaAccentGradient(): Brush {
    val colors = MaterialTheme.colorScheme
    return remember(colors) {
        SolidColor(colors.primary)
    }
}

/**
 * Clean minimalist tonal card with subtle 1dp hairline border and restrained press feedback.
 */
@Composable
fun FazaGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    accented: Boolean = false,
    highlightedBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (accented) colors.surfaceContainer else colors.surfaceContainerLow
    val contentColor = colors.onSurface
    val borderStroke = when {
        highlightedBorder -> BorderStroke(width = 1.25.dp, color = colors.primary)
        accented -> BorderStroke(width = 1.dp, color = colors.primary.copy(alpha = 0.28f))
        else -> BorderStroke(width = 1.dp, color = colors.outlineVariant.copy(alpha = 0.55f))
    }

    val interactiveModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressScale = rememberFazaBounceScale(
            interactionSource = interactionSource,
            enabled = true,
            pressedScale = 0.98f
        )
        Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
    } else {
        Modifier.clip(shape)
    }

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = borderStroke,
        modifier = modifier.then(interactiveModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Clean minimalist icon tile with subtle tonal fill instead of heavy saturated circles.
 */
@Composable
fun FazaGlassIconBadge(
    icon: ImageVector,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 12.dp,
    useBlueHighlight: Boolean = false,
    useSolidGradient: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius.coerceAtMost(14.dp))
    val bgColor = when {
        useSolidGradient -> colors.primary.copy(alpha = 0.14f)
        useBlueHighlight -> colors.surfaceContainerHigh
        else -> colors.surfaceContainerHigh
    }
    val iconTint = when {
        useSolidGradient -> colors.primary
        useBlueHighlight -> colors.onSurfaceVariant
        else -> colors.primary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Minimalist top-bar icon button with quiet hover/active styling and 48dp touch target.
 */
@Composable
fun FazaGlassTopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val bounceScale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = true,
        pressedScale = 0.92f
    )
    val fillColor = if (highlighted) {
        colors.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }
    val iconTint = if (highlighted) colors.primary else colors.onSurfaceVariant

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(fillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Minimalist pill primary button with crisp high-contrast fill and tactile press response.
 */
@Composable
fun FazaAccentGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberFazaBounceScale(
        interactionSource = interactionSource,
        enabled = enabled,
        pressedScale = 0.96f
    )
    val containerColor = if (enabled) colors.primary else colors.surfaceContainerHighest
    val contentColor = if (enabled) colors.onPrimary else colors.onSurfaceVariant

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 48.dp)
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/**
 * Understated minimalist micro-badge for counts, states, and tags.
 */
@Composable
fun FazaGlassPillBadge(
    text: String,
    useBlueAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val bgColor = if (useBlueAccent) {
        colors.surfaceContainerHigh
    } else {
        colors.primary.copy(alpha = 0.12f)
    }
    val textColor = if (useBlueAccent) colors.onSurfaceVariant else colors.primary

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
