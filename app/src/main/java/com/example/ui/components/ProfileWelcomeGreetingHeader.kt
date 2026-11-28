package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.ProfileWelcomeGreeting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val GREETING_AUTO_DISMISS_MS = 6_500L

/**
 * Animated Profile header onboarding greeting that reveals "Welcome, [User Name]"
 * with staggered spring motion, an expanding halo ring, and a subtle light sweep
 * upon a successful Google Sign-In.
 */
@Composable
fun ProfileWelcomeGreetingHeader(
    greeting: ProfileWelcomeGreeting?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = GREETING_AUTO_DISMISS_MS
) {
    var displayedGreeting by remember { mutableStateOf(greeting) }
    if (greeting != null) {
        displayedGreeting = greeting
    }

    LaunchedEffect(greeting?.id) {
        if (greeting != null && autoDismissMillis > 0L) {
            delay(autoDismissMillis)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = greeting != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 240)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) { fullHeight -> -fullHeight / 2 } +
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
            slideOutVertically(animationSpec = tween(durationMillis = 200)) { fullHeight -> -fullHeight / 3 } +
            shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 220)
            ),
        modifier = modifier
    ) {
        val activeGreeting = displayedGreeting ?: return@AnimatedVisibility
        ProfileWelcomeGreetingCardContent(
            greeting = activeGreeting,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ProfileWelcomeGreetingCardContent(
    greeting: ProfileWelcomeGreeting,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val welcomeText = stringResource(R.string.profile_welcome_greeting, greeting.userName)
    val subtitleText = stringResource(R.string.profile_welcome_greeting_subtitle)

    val avatarScale = remember(greeting.id) { Animatable(0.72f) }
    val badgeRotation = remember(greeting.id) { Animatable(-28f) }
    val pulseProgress = remember(greeting.id) { Animatable(0f) }
    val shimmerProgress = remember(greeting.id) { Animatable(-0.35f) }
    val titleAlpha = remember(greeting.id) { Animatable(0f) }
    val titleOffsetPx = remember(greeting.id) { Animatable(18f) }
    val subtitleAlpha = remember(greeting.id) { Animatable(0f) }

    LaunchedEffect(greeting.id) {
        launch {
            avatarScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(50L)
            badgeRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            pulseProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(60L)
            launch {
                titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 260))
            }
            titleOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            delay(140L)
            subtitleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 280))
        }
        launch {
            delay(100L)
            shimmerProgress.animateTo(
                targetValue = 1.35f,
                animationSpec = tween(durationMillis = 920, easing = FastOutSlowInEasing)
            )
        }
    }

    val containerBrush = Brush.linearGradient(
        colors = listOf(
            colors.primaryContainer.copy(alpha = 0.92f),
            colors.secondaryContainer.copy(alpha = 0.88f),
            colors.surfaceContainerHigh.copy(alpha = 0.95f)
        )
    )
    val shimmerColor = colors.primary.copy(alpha = 0.16f)
    val haloColor = colors.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.34f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_welcome_greeting_header")
            .semantics { contentDescription = welcomeText }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
                .drawBehind {
                    val sweep = shimmerProgress.value
                    if (sweep in -0.3f..1.3f) {
                        val bandWidth = size.width * 0.38f
                        val startX = size.width * sweep - bandWidth * 0.5f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    shimmerColor.copy(alpha = 0f),
                                    shimmerColor,
                                    shimmerColor.copy(alpha = 0f)
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(startX + bandWidth, size.height)
                            )
                        )
                    }
                }
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            val p = pulseProgress.value
                            if (p > 0f && p < 1f) {
                                val radius = (size.minDimension * 0.42f) + (size.minDimension * 0.24f * p)
                                drawCircle(
                                    color = haloColor.copy(alpha = (1f - p) * 0.42f),
                                    radius = radius,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                ) {
                    GoogleAccountAvatar(
                        photoUrl = greeting.photoUrl,
                        displayName = greeting.userName,
                        size = 42.dp,
                        modifier = Modifier.scale(avatarScale.value)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .scale(avatarScale.value)
                            .rotate(badgeRotation.value)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.profile_welcome_greeting_badge_cd),
                            tint = colors.onPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = welcomeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = titleOffsetPx.value.roundToInt()) }
                            .alpha(titleAlpha.value)
                            .testTag("profile_welcome_greeting_text")
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .alpha(subtitleAlpha.value)
                            .testTag("profile_welcome_greeting_subtitle")
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_welcome_greeting_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.profile_welcome_greeting_dismiss_cd),
                        tint = colors.onPrimaryContainer.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact TopAppBar greeting displayed when the AI Chat subtab is active so the
 * "Welcome, [User Name]" onboarding animation appears in the Profile header without
 * consuming vertical space above the chat viewport.
 */
@Composable
fun ProfileCompactHeaderGreeting(
    greeting: ProfileWelcomeGreeting,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = GREETING_AUTO_DISMISS_MS
) {
    val colors = MaterialTheme.colorScheme
    val welcomeText = stringResource(R.string.profile_welcome_greeting, greeting.userName)
    val badgeScale = remember(greeting.id) { Animatable(0.75f) }

    LaunchedEffect(greeting.id) {
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        if (autoDismissMillis > 0L) {
            delay(autoDismissMillis)
            onDismiss()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_compact_welcome_greeting")
            .semantics { contentDescription = welcomeText }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(badgeScale.value)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = stringResource(R.string.profile_welcome_greeting_badge_cd),
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = welcomeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("profile_compact_welcome_greeting_text")
            )
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

