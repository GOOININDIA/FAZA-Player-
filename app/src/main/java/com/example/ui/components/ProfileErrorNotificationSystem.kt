package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.AuthErrorCategory
import com.example.auth.FazaAuthUiError

/**
 * Custom Material 3 [SnackbarVisuals] carrying structured [FazaAuthUiError] metadata so
 * [ProfileNotificationSnackbarHost] renders tailored icons, titles, and recovery actions.
 */
data class ProfileErrorSnackbarVisuals(
    val uiError: FazaAuthUiError,
    override val actionLabel: String? = resolveAuthErrorActionLabel(uiError.category),
    override val duration: SnackbarDuration = SnackbarDuration.Long,
    override val withDismissAction: Boolean = true
) : SnackbarVisuals {
    override val message: String
        get() = uiError.message
}

fun resolveAuthErrorActionLabel(category: AuthErrorCategory): String? = when (category) {
    AuthErrorCategory.SESSION_EXPIRED -> "Sign In"
    AuthErrorCategory.NETWORK_TIMEOUT,
    AuthErrorCategory.SIGN_IN_FAILED,
    AuthErrorCategory.ACCOUNT_UNAVAILABLE -> "Retry"
    AuthErrorCategory.SIGN_OUT_FAILED -> null
}

private fun resolveAuthErrorIcon(category: AuthErrorCategory): ImageVector = when (category) {
    AuthErrorCategory.NETWORK_TIMEOUT -> Icons.Filled.WifiOff
    AuthErrorCategory.SESSION_EXPIRED -> Icons.Filled.LockClock
    AuthErrorCategory.ACCOUNT_UNAVAILABLE -> Icons.Filled.AccountCircle
    AuthErrorCategory.SIGN_IN_FAILED,
    AuthErrorCategory.SIGN_OUT_FAILED -> Icons.Filled.ErrorOutline
}

@Composable
private fun resolveErrorSurfacePalette(category: AuthErrorCategory): Triple<Color, Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (category) {
        AuthErrorCategory.SESSION_EXPIRED -> Triple(
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
            colors.tertiary.copy(alpha = 0.28f)
        )
        AuthErrorCategory.ACCOUNT_UNAVAILABLE -> Triple(
            colors.secondaryContainer,
            colors.onSecondaryContainer,
            colors.secondary.copy(alpha = 0.26f)
        )
        AuthErrorCategory.NETWORK_TIMEOUT,
        AuthErrorCategory.SIGN_IN_FAILED,
        AuthErrorCategory.SIGN_OUT_FAILED -> Triple(
            colors.errorContainer,
            colors.onErrorContainer,
            colors.error.copy(alpha = 0.26f)
        )
    }
}

/**
 * Dedicated Profile error boundary banner rendered persistently at the top of the Profile screen
 * (across both Overview and FAZA AI Chat subtabs) whenever an auth failure, network timeout, or
 * session expiration occurs, ensuring recovery actions stay visible even after scrolling or after
 * the floating snackbar auto-dismisses.
 */
@Composable
fun ProfileErrorBoundaryBanner(
    uiError: FazaAuthUiError?,
    isBusy: Boolean,
    onRetrySignIn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boundaryDescription = stringResource(R.string.profile_error_boundary_cd)

    AnimatedVisibility(
        visible = uiError != null,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        uiError?.let { error ->
            val (containerColor, contentColor, borderColor) = resolveErrorSurfacePalette(error.category)
            val icon = resolveAuthErrorIcon(error.category)
            val actionLabel = if (error.canRetry) resolveAuthErrorActionLabel(error.category) else null

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = containerColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, borderColor),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = boundaryDescription }
                    .testTag("profile_error_boundary_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = error.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("profile_error_boundary_title")
                            )
                            Text(
                                text = error.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.92f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("profile_error_boundary_message")
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("profile_error_boundary_dismiss")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.profile_error_dismiss_cd),
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (actionLabel != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(
                                onClick = onRetrySignIn,
                                enabled = !isBusy,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = contentColor.copy(alpha = 0.14f),
                                    contentColor = contentColor
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_error_boundary_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (error.category == AuthErrorCategory.SESSION_EXPIRED) {
                                        stringResource(R.string.profile_error_sign_in_again_action)
                                    } else {
                                        stringResource(R.string.profile_error_retry_action)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating Material 3 snackbar notification host dedicated to Profile authentication, network
 * timeout, and session expiration feedback. Automatically clears the bottom navigation dock and
 * system navigation bar safe areas.
 */
@Composable
fun ProfileNotificationSnackbarHost(
    hostState: SnackbarHostState,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding + 12.dp)
    ) { snackbarData ->
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ProfileAuthSnackbarCard(snackbarData = snackbarData)
        }
    }
}

@Composable
private fun ProfileAuthSnackbarCard(snackbarData: SnackbarData) {
    val customVisuals = snackbarData.visuals as? ProfileErrorSnackbarVisuals
    val category = customVisuals?.uiError?.category ?: AuthErrorCategory.SIGN_IN_FAILED
    val title = customVisuals?.uiError?.title ?: "Notice"
    val message = snackbarData.visuals.message
    val actionLabel = snackbarData.visuals.actionLabel
    val icon = resolveAuthErrorIcon(category)
    val colors = MaterialTheme.colorScheme
    val accentTint = when (category) {
        AuthErrorCategory.SESSION_EXPIRED -> colors.tertiary
        AuthErrorCategory.ACCOUNT_UNAVAILABLE -> colors.secondary
        AuthErrorCategory.NETWORK_TIMEOUT,
        AuthErrorCategory.SIGN_IN_FAILED,
        AuthErrorCategory.SIGN_OUT_FAILED -> colors.error
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("profile_error_snackbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentTint.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.inversePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("profile_error_snackbar_title")
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inverseOnSurface.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("profile_error_snackbar_message")
                )
            }

            if (!actionLabel.isNullOrBlank()) {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.inversePrimary
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("profile_error_snackbar_action")
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (snackbarData.visuals.withDismissAction) {
                IconButton(
                    onClick = { snackbarData.dismiss() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_error_snackbar_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.profile_error_dismiss_cd),
                        tint = colors.inverseOnSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
